#!/usr/bin/env python3
import json, os, statistics, sys, time, urllib.request
from collections import Counter
from pathlib import Path

SMALL_MODEL="llama3.2:3b"; LARGE_MODEL="qwen2.5:7b-instruct"; CONFIDENCE_THRESHOLD=.80
LONG_REQUEST_THRESHOLD=400
MARKERS=("проанализируй архитектуру","подробный план","сравни несколько","противоречив","миграция","race condition","выбери оптимальный","предложи стратегию")
SMALL_PROMPT='''You are the first tier in a model-routing system.
Answer the latest user request and assess whether your answer is reliable and sufficient.
Return exactly one JSON object with exactly six fields: answer, confidence, needs_escalation, ambiguity, sufficient_context, reason.
answer and reason must be non-empty strings. confidence must be a number from 0.0 to 1.0.
needs_escalation and sufficient_context must be booleans. ambiguity must be LOW, MEDIUM, or HIGH.
Return no Markdown fences and no text before or after JSON.
Use LOW when there is one clear interpretation and a reliable answer. Use MEDIUM when there are
multiple plausible interpretations or solutions. Use HIGH when information conflicts or a reliable
answer cannot be determined. Set sufficient_context=false when reliable answering requires more data,
logs, configuration, environment details, or requirements. Set needs_escalation=true whenever ambiguity
is not LOW or sufficient_context is false. Do not guess an exact cause when data is insufficient, and do
not choose one of several possibilities merely because it seems most likely.
Example:
{"answer":"In Kotlin, val cannot be reassigned after initialization, while var can.","confidence":0.98,"needs_escalation":false,"ambiguity":"LOW","sufficient_context":true,"reason":"A simple question about basic Kotlin syntax."}'''
LARGE_PROMPT="Ты модель второго уровня. Дай точный, полный и понятный ответ. Не упоминай routing, confidence, fallback, модели или эскалацию."
SMALL_SCHEMA={"type":"object","properties":{"answer":{"type":"string","minLength":1},"confidence":{"type":"number","minimum":0.0,"maximum":1.0},"needs_escalation":{"type":"boolean"},"ambiguity":{"type":"string","enum":["LOW","MEDIUM","HIGH"]},"sufficient_context":{"type":"boolean"},"reason":{"type":"string","minLength":1}},"required":["answer","confidence","needs_escalation","ambiguity","sufficient_context","reason"],"additionalProperties":False}
ROOT=Path(__file__).resolve().parents[1]; BASE=os.getenv("OLLAMA_BASE_URL","http://localhost:11434").rstrip("/")

def call(model, system, text, tokens, temperature=.2, schema=None):
    payload={"model":model,"prompt":text,"system":system,"stream":False,"options":{"temperature":temperature,"num_predict":tokens}}
    if schema is not None: payload["format"]=schema
    body=json.dumps(payload).encode(); start=time.perf_counter(); req=urllib.request.Request(BASE+"/api/generate",body,{"Content-Type":"application/json"})
    with urllib.request.urlopen(req, timeout=300) as response: data=json.load(response)
    return data["response"].strip(), round((time.perf_counter()-start)*1000)

def parse(raw):
    text=raw.strip()
    if text.startswith("```") and text.endswith("```"): text=text.split("\n",1)[1].rsplit("```",1)[0].strip()
    start=text.find("{"); end=text.rfind("}")
    if start>=0 and end>=start: text=text[start:end+1]
    value=json.loads(text); required=("answer","confidence","needs_escalation","ambiguity","sufficient_context","reason")
    if not isinstance(value,dict) or any(k not in value for k in required): raise ValueError("missing field")
    if not isinstance(value["answer"],str) or not value["answer"].strip(): raise ValueError("invalid answer")
    c=value["confidence"]
    if isinstance(c,str):
        try: c=float(c)
        except ValueError as error: raise ValueError("invalid confidence") from error
    if isinstance(c,bool) or not isinstance(c,(int,float)) or not 0<=c<=1: raise ValueError("invalid confidence")
    if type(value["needs_escalation"]) is not bool: raise ValueError("invalid escalation")
    if value["ambiguity"] not in ("LOW","MEDIUM","HIGH"): raise ValueError("invalid ambiguity")
    if type(value["sufficient_context"]) is not bool: raise ValueError("invalid sufficient context")
    if not isinstance(value["reason"],str) or not value["reason"].strip(): raise ValueError("invalid reason")
    value["confidence"]=c; return value

def direct_reason(text):
    if len(text)>LONG_REQUEST_THRESHOLD:return "LONG_REQUEST"
    if any(m in text.lower() for m in MARKERS):return "COMPLEX_REQUEST"

def route(case):
    total=time.perf_counter(); text=case["input"]; reason=direct_reason(text); small_ms=large_ms=confidence=None
    if not reason:
        try:
            raw,small_ms=call(SMALL_MODEL,SMALL_PROMPT,text,180,temperature=0.0,schema=SMALL_SCHEMA); decision=parse(raw); confidence=decision["confidence"]
            if decision["needs_escalation"]:reason="MODEL_REQUESTED_ESCALATION"
            elif confidence<CONFIDENCE_THRESHOLD:reason="LOW_CONFIDENCE"
            elif decision["ambiguity"]!="LOW" or not decision["sufficient_context"]:reason="MODEL_REQUESTED_ESCALATION"
            else: answer=decision["answer"].strip()
        except (ValueError,json.JSONDecodeError):reason="INVALID_JSON"
        except Exception:reason="SMALL_MODEL_ERROR"
    if reason: answer,large_ms=call(LARGE_MODEL,LARGE_PROMPT,text,1000); actual="LARGE"; final=LARGE_MODEL
    else: actual="SMALL"; final=SMALL_MODEL
    return {**case,"actualRoute":actual,"smallModelConfidence":confidence,"escalated":actual=="LARGE","escalationReason":reason,"finalModel":final,"smallModelLatencyMs":small_ms,"largeModelLatencyMs":large_ms,"totalLatencyMs":round((time.perf_counter()-total)*1000),"success":actual==case["expectedRoute"],"answerPreview":answer[:120]}

def avg(rows,key):
    values=[r[key] for r in rows if r[key] is not None]; return round(statistics.mean(values)) if values else None

def main():
    cases=[json.loads(line) for line in (ROOT/"data/test-cases.jsonl").read_text(encoding="utf-8-sig").splitlines() if line.strip()]; rows=[route(c) for c in cases]; reasons=Counter(r["escalationReason"] for r in rows if r["escalationReason"]); accuracy=sum(r["success"] for r in rows)
    lines=["# Model routing report","",f"Small model: {SMALL_MODEL}",f"Large model: {LARGE_MODEL}",f"Confidence threshold: {CONFIDENCE_THRESHOLD:.2f}","",f"Examples: {len(rows)}",f"Stayed on small model: {sum(not r['escalated'] for r in rows)}",f"Escalated to large model: {sum(r['escalated'] for r in rows)}",f"Routing accuracy: {accuracy}/{len(rows)}","","Escalation reasons:"]
    for reason in ("COMPLEX_REQUEST","LONG_REQUEST","LOW_CONFIDENCE","MODEL_REQUESTED_ESCALATION","INVALID_JSON","SMALL_MODEL_ERROR"): lines.append(f"{reason}: {reasons[reason]}")
    lines += ["",f"Average small model latency: {avg(rows,'smallModelLatencyMs')} ms",f"Average large model latency: {avg(rows,'largeModelLatencyMs')} ms",f"Average total latency: {avg(rows,'totalLatencyMs')} ms","","| id | expected | actual | confidence | reason | final model | latency |","|---|---|---|---:|---|---|---:|"]
    for r in rows: lines.append(f"| {r['id']} | {r['expectedRoute']} | {r['actualRoute']} | {r['smallModelConfidence'] if r['smallModelConfidence'] is not None else '-'} | {r['escalationReason'] or '-'} | {r['finalModel']} | {r['totalLatencyMs']} ms |")
    report="\n".join(lines)+"\n"; print(report); (ROOT/"reports/latest.md").write_text(report,encoding="utf-8"); (ROOT/"reports/latest.json").write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding="utf-8")
if __name__=="__main__":
    try: main()
    except Exception as error: print(f"Routing runner failed: {error}",file=sys.stderr); sys.exit(1)
