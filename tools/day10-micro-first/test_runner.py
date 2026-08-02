import json,tempfile,unittest
from collections import Counter
from pathlib import Path
from unittest.mock import patch
import run_micro_first as runner
ROOT=Path(__file__).parent

def row(**updates):
    value={field:None for field in runner.RESULT_FIELDS}; value.update({"id":"x","group":"simple","input":"x","expected_label":"NETWORK_UNAVAILABLE","expected_route":"MICRO","micro_label":"NETWORK_UNAVAILABLE","top_score":.9,"second_score":.1,"margin":.8,"micro_status":"OK","actual_route":"MICRO","final_label":"NETWORK_UNAVAILABLE","correct_label":True,"correct_route":True,"fallback_reason":None,"micro_latency_ms":2.0,"fallback_latency_ms":0.0,"total_latency_ms":2.0,"large_llm_calls":0,"error":None}); value.update(updates); return value

class ContractTests(unittest.TestCase):
 def test_dataset_exact_fields_and_groups(self):
  prototypes,cases=runner.load_inputs(ROOT/"data"); self.assertTrue(all(len(set(v))>=8 for v in prototypes.values())); self.assertEqual(30,len(cases)); self.assertEqual({"simple":10,"boundary":10,"complex-noisy":10},Counter(x["group"] for x in cases)); self.assertTrue(all(set(x)=={"id","group","input","expected_label","expected_route"} for x in cases)); self.assertTrue(all(x["expected_route"]=="FALLBACK" for x in cases if x["expected_label"]=="AMBIGUOUS"))
 def test_exact_models_and_result_fields(self):
  self.assertEqual("nomic-embed-text:latest",runner.MICRO_MODEL); self.assertEqual("qwen2.5:7b-instruct",runner.FALLBACK_MODEL); self.assertEqual(20,len(runner.RESULT_FIELDS)); self.assertEqual(set(runner.RESULT_FIELDS),set(row()))
 def test_fallback_schema_and_russian_validation(self):
  schema=runner.fallback_schema(); self.assertNotIn("enum",schema["properties"]["user_action"]); self.assertEqual([0,1],[schema["properties"]["confidence"]["minimum"],schema["properties"]["confidence"]["maximum"]])
  valid={"category":"AMBIGUOUS","confidence":.4,"title":"Неясная ошибка","message":"Причина не определена","user_action":"Повторите попытку"}; self.assertEqual(valid,runner.parse_fallback_response(json.dumps(valid,ensure_ascii=False)))
  with self.assertRaises(ValueError): runner.parse_fallback_response(json.dumps({**valid,"message":"English only"},ensure_ascii=False))
 def test_fallback_payload_has_no_embeddings_or_prototypes(self):
  valid={"category":"AMBIGUOUS","confidence":.4,"title":"Неясно","message":"Причина неизвестна","user_action":"Уточните ошибку"}
  with self.assertRaises(ValueError): runner.parse_fallback_response(json.dumps({**valid,"user_action":"Нажмите RETRY_ACTION"},ensure_ascii=False))
  with patch.object(runner,"post_json",return_value={"response":json.dumps(valid,ensure_ascii=False)}) as call:
   original="исходный текст"; runner.fallback_classify("http://local",runner.FALLBACK_MODEL,original,1); payload=call.call_args.args[2]; self.assertEqual(original,payload["prompt"]); self.assertIn("system",payload); self.assertEqual({"temperature":0},payload["options"]); self.assertNotIn("embeddings",json.dumps(payload)); self.assertNotIn("prototypes",json.dumps(payload))

class MathMetricsTests(unittest.TestCase):
 def test_math_and_threshold_reasons(self):
  self.assertEqual([.6,.8],runner.normalize([3,4])); self.assertAlmostEqual(1,runner.cosine([1,0],[2,0])); decision=runner.micro_decision([1,1],{"A":[1,0],"B":[0,1]}); self.assertEqual("LOW_MARGIN",decision["fallback_reason"]); self.assertEqual("UNSURE",decision["micro_status"])
  with self.assertRaises(runner.InvalidVectorError): runner.normalize([float("nan")])
  with self.assertRaises(runner.InvalidVectorError): runner.normalize([float("inf")])
  self.assertEqual(("LOW_SCORE","LOW_MARGIN","EMBEDDING_ERROR","INVALID_VECTOR","PROTOTYPE_INITIALIZATION_ERROR","MICRO_RESULT_INVALID"),runner.FALLBACK_REASONS)
 def test_all_metrics_and_incorrect_confident_accept(self):
  rows=[row(),row(id="bad",micro_label="OPENAI_TIMEOUT",final_label="OPENAI_TIMEOUT",correct_label=False,total_latency_ms=4),row(id="fb",group="complex-noisy",expected_label="AMBIGUOUS",expected_route="FALLBACK",micro_status="UNSURE",actual_route="FALLBACK",final_label="AMBIGUOUS",correct_label=True,correct_route=True,fallback_reason="LOW_MARGIN",micro_latency_ms=1,fallback_latency_ms=9,total_latency_ms=10,large_llm_calls=1)]
  metrics=runner.calculate_metrics(rows); self.assertEqual(1,metrics["incorrect_micro_accepts"]); self.assertEqual(.5,metrics["incorrect_micro_accept_rate"]); self.assertEqual(1,metrics["fallback_reason_counts"]["LOW_MARGIN"]); self.assertEqual(set(runner.FALLBACK_REASONS),set(metrics["fallback_reason_counts"])); self.assertIn("p95_latency_ms",metrics)
  with tempfile.TemporaryDirectory() as directory:
   output=Path(directory); runner.write_reports(rows,output); parsed=[json.loads(x) for x in (output/"results.jsonl").read_text(encoding="utf-8").splitlines()]; self.assertTrue(all(list(x)==list(runner.RESULT_FIELDS) for x in parsed)); summary=(output/"summary.txt").read_text(encoding="utf-8"); self.assertTrue(all(label in summary for label in ("Examples:","Handled by micro-model:","P95 latency:","Incorrect micro accepts:","Group complex-noisy:")))

if __name__=="__main__": unittest.main()
