#!/usr/bin/env python3
"""Run the Day 10 micro-first evaluation against a local Ollama server."""
import argparse,json,math,re,statistics,time,urllib.error,urllib.request
from collections import Counter,defaultdict
from pathlib import Path

LABELS=("NETWORK_UNAVAILABLE","OPENAI_RATE_LIMIT","OPENAI_TIMEOUT","EMPTY_AI_RESPONSE","LOCAL_HISTORY_UNAVAILABLE")
ALL_LABELS=LABELS+("AMBIGUOUS",)
MICRO_MODEL="nomic-embed-text:latest"
FALLBACK_MODEL="qwen2.5:7b-instruct"
MIN_SCORE=.70
MIN_MARGIN=.06
FALLBACK_REASONS=("LOW_SCORE","LOW_MARGIN","EMBEDDING_ERROR","INVALID_VECTOR","PROTOTYPE_INITIALIZATION_ERROR","MICRO_RESULT_INVALID")
RESULT_FIELDS=("id","group","input","expected_label","expected_route","micro_label","top_score","second_score","margin","micro_status","actual_route","final_label","correct_label","correct_route","fallback_reason","micro_latency_ms","fallback_latency_ms","total_latency_ms","large_llm_calls","error")

class FallbackError(RuntimeError):
    def __init__(self,message,calls):
        super().__init__(message); self.calls=calls

class EmbeddingTransportError(RuntimeError): pass
class InvalidVectorError(ValueError): pass

def normalize(vector):
    values=[float(x) for x in vector]
    if not values or any(not math.isfinite(x) for x in values): raise InvalidVectorError("embedding must contain finite values")
    norm=math.sqrt(sum(x*x for x in values))
    if not math.isfinite(norm) or norm==0: raise InvalidVectorError("embedding must be a finite non-zero vector")
    return [x/norm for x in values]

def cosine(left,right):
    left,right=normalize(left),normalize(right)
    if len(left)!=len(right): raise InvalidVectorError("embedding dimensions differ")
    return sum(x*y for x,y in zip(left,right))

def centroid(vectors):
    vectors=[normalize(v) for v in vectors]
    if not vectors: raise InvalidVectorError("centroid requires vectors")
    if any(len(v)!=len(vectors[0]) for v in vectors): raise InvalidVectorError("embedding dimensions differ")
    return normalize(sum(v[i] for v in vectors) for i in range(len(vectors[0])))

def micro_decision(vector,prototype_vectors,min_score=MIN_SCORE,min_margin=MIN_MARGIN):
    ranked=sorted(((label,max(cosine(vector,prototype) for prototype in vectors)) for label,vectors in prototype_vectors.items()),key=lambda x:(-x[1],x[0]))
    label,top=ranked[0]; second=ranked[1][1]; margin=top-second
    low_score=top<min_score; low_margin=margin<min_margin
    reason="LOW_SCORE" if low_score else "LOW_MARGIN" if low_margin else None
    return {"micro_label":label,"top_score":top,"second_score":second,"margin":margin,"micro_status":"UNSURE" if reason else "OK","fallback_reason":reason}

def validate_micro_result(result):
    required={"micro_label","top_score","second_score","margin","micro_status","fallback_reason"}
    if not isinstance(result,dict) or set(result)!=required or result.get("micro_label") not in LABELS or result.get("micro_status") not in {"OK","UNSURE"}: raise ValueError("malformed micro result")
    if any(isinstance(result.get(field),bool) or not isinstance(result.get(field),(int,float)) or not math.isfinite(result[field]) for field in ("top_score","second_score","margin")): raise ValueError("malformed micro scores")
    if result["fallback_reason"] not in {None,"LOW_SCORE","LOW_MARGIN"}: raise ValueError("malformed fallback reason")
    return result

def post_json(base_url,path,payload,timeout):
    request=urllib.request.Request(base_url.rstrip("/")+path,json.dumps(payload).encode("utf-8"),{"Content-Type":"application/json"},method="POST")
    try:
        with urllib.request.urlopen(request,timeout=timeout) as response: return json.loads(response.read().decode("utf-8"))
    except (urllib.error.URLError,TimeoutError,json.JSONDecodeError) as error: raise RuntimeError(f"Ollama {path} failed: {error}") from error

def embed_batch(base_url,model,texts,timeout):
    try: embeddings=post_json(base_url,"/api/embed",{"model":model,"input":texts},timeout).get("embeddings")
    except RuntimeError as error: raise EmbeddingTransportError(str(error)) from error
    if not isinstance(embeddings,list) or len(embeddings)!=len(texts): raise EmbeddingTransportError("invalid /api/embed batch")
    normalized=[normalize(vector) for vector in embeddings]
    if normalized and any(len(vector)!=len(normalized[0]) for vector in normalized): raise InvalidVectorError("embedding batch dimensions differ")
    return normalized

def fallback_schema():
    reason={"type":"string","minLength":1,"maxLength":160}
    return {"type":"object","properties":{"category":{"type":"string","enum":list(ALL_LABELS)},"confidence":{"type":"number","minimum":0,"maximum":1},"reason":reason},"required":["category","confidence","reason"],"additionalProperties":False}

def parse_fallback_response(raw):
    try: value=json.loads(raw)
    except json.JSONDecodeError as error: raise ValueError("fallback response is not JSON") from error
    if not isinstance(value,dict) or set(value)!=set(fallback_schema()["required"]): raise ValueError("fallback fields do not match schema")
    if value["category"] not in ALL_LABELS or isinstance(value["confidence"],bool) or not isinstance(value["confidence"],(int,float)) or not 0<=value["confidence"]<=1: raise ValueError("invalid category or confidence")
    if not isinstance(value["reason"],str) or not value["reason"].strip() or not re.search("[А-Яа-яЁё]",value["reason"]): raise ValueError("reason must be nonblank Russian text")
    if len(value["reason"])>160: raise ValueError("reason must not exceed 160 characters")
    return value

def fallback_classify(base_url,model,input_text,timeout):
    system=("Use a cause-first algorithm: identify the reported failure cause, distinguish it from symptoms and advice, "
            "then choose exactly one category. NETWORK_UNAVAILABLE means absent network connectivity; "
            "OPENAI_RATE_LIMIT means excessive request frequency, volume, quota, throttling, or HTTP 429; "
            "OPENAI_TIMEOUT means elapsed duration, expired deadline, or no timely response; EMPTY_AI_RESPONSE means "
            "a completed response with no usable content; LOCAL_HISTORY_UNAVAILABLE means locally stored chat history "
            "cannot be read or restored; AMBIGUOUS means evidence is insufficient or supports multiple causes. "
            "A recommendation to retry later does not determine the category and, without a stated cause, is AMBIGUOUS. "
            "Return exactly category, confidence, and a short Russian reason explaining the cause.")
    for attempt in (1,2):
        current=system if attempt==1 else system+" Correct the previous invalid output; return only a valid object."
        try: response=post_json(base_url,"/api/generate",{"model":model,"system":current,"prompt":input_text,"stream":False,"format":fallback_schema(),"options":{"temperature":0}},timeout)
        except RuntimeError as error: raise FallbackError(str(error),attempt) from error
        try: return parse_fallback_response(response.get("response","")),attempt
        except ValueError:
            if attempt==2: raise FallbackError("fallback returned invalid structured output twice",2)
    raise AssertionError("unreachable")

def load_inputs(data_dir):
    prototypes=json.loads((data_dir/"prototypes.json").read_text(encoding="utf-8")); cases=[json.loads(line) for line in (data_dir/"test_cases.jsonl").read_text(encoding="utf-8").splitlines() if line.strip()]
    required={"id","group","input","expected_label","expected_route"}
    if set(prototypes)!=set(LABELS) or any(len(set(items))<8 for items in prototypes.values()): raise ValueError("five labels with at least eight unique prototypes required")
    if any(set(case)!=required for case in cases): raise ValueError("test-case fields do not match the contract")
    return prototypes,cases

def percentile95(values):
    if not values:return 0.0
    ordered=sorted(values); return ordered[max(0,math.ceil(.95*len(ordered))-1)]

def calculate_metrics(results):
    total=len(results); latencies=[r["total_latency_ms"] for r in results]; micro=[r for r in results if r["actual_route"]=="MICRO"]; fallback=[r for r in results if r["actual_route"]=="FALLBACK"]
    by_group={}
    for group in ("simple","boundary","complex-noisy"):
        rows=[r for r in results if r["group"]==group]; by_group[group]={"count":len(rows),"label_accuracy":sum(r["correct_label"] for r in rows)/len(rows) if rows else 0,"route_accuracy":sum(r["correct_route"] for r in rows)/len(rows) if rows else 0}
    incorrect=sum(not r["correct_label"] for r in micro)
    return {"examples":total,"handled_by_micro_model":len(micro),"fallback_count":len(fallback),"micro_coverage":len(micro)/total if total else 0,"large_llm_calls":sum(r["large_llm_calls"] for r in results),"average_latency_ms":statistics.fmean(latencies) if latencies else 0,"median_latency_ms":statistics.median(latencies) if latencies else 0,"p95_latency_ms":percentile95(latencies),"average_micro_only_latency_ms":statistics.fmean(r["total_latency_ms"] for r in micro) if micro else 0,"average_fallback_latency_ms":statistics.fmean(r["fallback_latency_ms"] for r in fallback) if fallback else 0,"overall_label_accuracy":sum(r["correct_label"] for r in results)/total if total else 0,"route_accuracy":sum(r["correct_route"] for r in results)/total if total else 0,"micro_accepted_accuracy":sum(r["correct_label"] for r in micro)/len(micro) if micro else 0,"incorrect_micro_accepts":incorrect,"incorrect_micro_accept_rate":incorrect/len(micro) if micro else 0,"fallback_accuracy":sum(r["correct_label"] for r in fallback)/len(fallback) if fallback else 0,"by_group":by_group,"fallback_reason_counts":{reason:sum(r["fallback_reason"]==reason for r in results) for reason in FALLBACK_REASONS}}

def write_reports(results,report_dir):
    report_dir.mkdir(parents=True,exist_ok=True)
    with (report_dir/"results.jsonl").open("w",encoding="utf-8",newline="\n") as output:
        for row in results: output.write(json.dumps({field:row[field] for field in RESULT_FIELDS},ensure_ascii=False)+"\n")
    m=calculate_metrics(results); lines=[f"Examples: {m['examples']}",f"Handled by micro-model: {m['handled_by_micro_model']}",f"Fallback count: {m['fallback_count']}",f"Micro coverage: {m['micro_coverage']:.4f}",f"Large LLM calls: {m['large_llm_calls']}",f"Average latency: {m['average_latency_ms']:.3f} ms",f"Median latency: {m['median_latency_ms']:.3f} ms",f"P95 latency: {m['p95_latency_ms']:.3f} ms",f"Average micro-only latency: {m['average_micro_only_latency_ms']:.3f} ms",f"Average fallback latency: {m['average_fallback_latency_ms']:.3f} ms",f"Overall label accuracy: {m['overall_label_accuracy']:.4f}",f"Route accuracy: {m['route_accuracy']:.4f}",f"Micro accepted accuracy: {m['micro_accepted_accuracy']:.4f}",f"Incorrect micro accepts: {m['incorrect_micro_accepts']} ({m['incorrect_micro_accept_rate']:.4f})",f"Fallback accuracy: {m['fallback_accuracy']:.4f}"]
    lines += [f"Group {name}: "+json.dumps(value,sort_keys=True) for name,value in m["by_group"].items()]
    lines += [f"Fallback reason {reason}: {m['fallback_reason_counts'][reason]}" for reason in FALLBACK_REASONS]
    (report_dir/"summary.txt").write_text("\n".join(lines)+"\n",encoding="utf-8")

def base_result(case):
    return {**case,"micro_label":None,"top_score":None,"second_score":None,"margin":None,"micro_status":"UNSURE","actual_route":"FALLBACK","final_label":None,"correct_label":False,"correct_route":case["expected_route"]=="FALLBACK","fallback_reason":"PROTOTYPE_INITIALIZATION_ERROR","micro_latency_ms":0.0,"fallback_latency_ms":0.0,"total_latency_ms":0.0,"large_llm_calls":0,"error":None}

def run(args):
    prototypes,cases=load_inputs(args.data_dir); flat=[(label,text) for label in LABELS for text in prototypes[label]]; references=None; setup_error=None
    try:
        vectors=embed_batch(args.base_url,args.micro_model,[item[1] for item in flat],args.timeout); grouped=defaultdict(list)
        for (label,_),vector in zip(flat,vectors): grouped[label].append(vector)
        references={label:grouped[label] for label in LABELS}
    except (RuntimeError,ValueError) as error: setup_error=str(error)
    results=[]
    for case in cases:
        total_start=time.perf_counter(); row=base_result(case); micro_start=time.perf_counter()
        try:
            if references is None: raise RuntimeError(setup_error or "prototype embeddings unavailable")
            vector=embed_batch(args.base_url,args.micro_model,[case["input"]],args.timeout)[0]
            try: decision=validate_micro_result(micro_decision(vector,references,args.min_score,args.min_margin))
            except InvalidVectorError: raise
            except (IndexError,KeyError,TypeError,ValueError) as error: row["fallback_reason"]="MICRO_RESULT_INVALID"; row["error"]="micro: "+str(error); decision=None
            if decision is not None: row.update(decision); row["actual_route"]="MICRO" if row["micro_status"]=="OK" else "FALLBACK"
        except EmbeddingTransportError as error: row["fallback_reason"]="EMBEDDING_ERROR"; row["error"]="micro: "+str(error)
        except InvalidVectorError as error: row["fallback_reason"]="INVALID_VECTOR"; row["error"]="micro: "+str(error)
        except RuntimeError as error: row["fallback_reason"]="PROTOTYPE_INITIALIZATION_ERROR"; row["error"]="micro: "+str(error)
        row["micro_latency_ms"]=round((time.perf_counter()-micro_start)*1000,3)
        if row["actual_route"]=="MICRO": row["final_label"]=row["micro_label"]
        else:
            fallback_start=time.perf_counter()
            try:
                presentation,calls=fallback_classify(args.base_url,args.fallback_model,case["input"],args.timeout); row["final_label"]=presentation["category"]; row["large_llm_calls"]=calls
            except FallbackError as error: row["large_llm_calls"]=error.calls; row["error"]=(row["error"]+"; " if row["error"] else "")+"fallback: "+str(error)
            row["fallback_latency_ms"]=round((time.perf_counter()-fallback_start)*1000,3)
        row["correct_label"]=row["final_label"]==case["expected_label"]; row["correct_route"]=row["actual_route"]==case["expected_route"]; row["total_latency_ms"]=round((time.perf_counter()-total_start)*1000,3); results.append(row)
    write_reports(results,args.report_dir); return results

def parser():
    root=Path(__file__).resolve().parent; value=argparse.ArgumentParser()
    value.add_argument("--base-url",default="http://127.0.0.1:11434"); value.add_argument("--micro-model",default=MICRO_MODEL); value.add_argument("--fallback-model",default=FALLBACK_MODEL); value.add_argument("--data-dir",type=Path,default=root/"data"); value.add_argument("--report-dir",type=Path,default=root/"reports"); value.add_argument("--min-score",type=float,default=MIN_SCORE); value.add_argument("--min-margin",type=float,default=MIN_MARGIN); value.add_argument("--timeout",type=float,default=60); return value

if __name__=="__main__": run(parser().parse_args())
