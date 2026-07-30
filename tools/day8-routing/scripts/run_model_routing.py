#!/usr/bin/env python3
import json, os, statistics, sys, time, urllib.request
from collections import Counter
from pathlib import Path

SMALL_MODEL="llama3.2:3b"; LARGE_MODEL="qwen2.5:7b-instruct"; CONFIDENCE_THRESHOLD=.80
MIN_ANSWER_LENGTH=20; LONG_REQUEST_THRESHOLD=400
MARKERS=("проанализируй архитектуру","подробный план","сравни несколько","противоречив","миграция","race condition","выбери оптимальный","предложи стратегию")
SMALL_PROMPT='''Ты модель первого уровня в системе маршрутизации. Ответь на запрос и верни только JSON: {"answer":"ответ","confidence":0.0,"needs_escalation":false,"reason":"объяснение"}. confidence — число 0.0..1.0, needs_escalation — boolean, answer и reason непустые. Эскалируй сложные, неоднозначные и ненадёжные ответы. Не используй Markdown.'''
LARGE_PROMPT="Ты модель второго уровня. Дай точный, полный и понятный ответ. Не упоминай routing, confidence, fallback, модели или эскалацию."
ROOT=Path(__file__).resolve().parents[1]; BASE=os.getenv("OLLAMA_BASE_URL","http://localhost:11434").rstrip("/")

def call(model, system, text, tokens):
    body=json.dumps({"model":model,"prompt":text,"system":system,"stream":False,"options":{"temperature":.2,"num_predict":tokens}}).encode()
    start=time.perf_counter(); req=urllib.request.Request(BASE+"/api/generate",body,{"Content-Type":"application/json"})
    with urllib.request.urlopen(req, timeout=300) as response: data=json.load(response)
    return data["response"].strip(), round((time.perf_counter()-start)*1000)

def parse(raw):
    text=raw.strip()
    if text.startswith("```") and text.endswith("```"):
        text=text.split("\n",1)[1].rsplit("```",1)[0].strip()
    value=json.loads(text)
    required=("answer","confidence","needs_escalation","reason")
    if not isinstance(value,dict) or any(k not in value for k in required): raise ValueError("missing field")
    if not isinstance(value["answer"],str) or not value["answer"].strip(): raise ValueError("invalid answer")
    c=value["confidence"]
    if isinstance(c,bool) or not isinstance(c,(int,float)) or not 0<=c<=1: raise ValueError("invalid confidence")
    if type(value["needs_escalation"]) is not bool: raise ValueError("invalid escalation")
    if not isinstance(value["reason"],str) or not value["reason"].strip(): raise ValueError("invalid reason")
    return value

def direct_reason(text):
    if len(text)>LONG_REQUEST_THRESHOLD:return "LONG_REQUEST"
    if any(m in text.lower() for m in MARKERS):return "COMPLEX_REQUEST"

def route(case):
    total=time.perf_counter(); text=case["input"]; reason=direct_reason(text); small_ms=large_ms=confidence=None; raw=None
    if not reason:
        try:
            raw,small_ms=call(SMALL_MODEL,SMALL_PROMPT,text,700); decision=parse(raw); confidence=decision["confidence"]
            if decision["needs_escalation"]:reason="MODEL_REQUESTED_ESCALATION"
            elif confidence<CONFIDENCE_THRESHOLD:reason="LOW_CONFIDENCE"
            elif len(decision["answer"].strip())<MIN_ANSWER_LENGTH:reason="ANSWER_TOO_SHORT"
            else: answer=decision["answer"].strip()
        except (ValueError,json.JSONDecodeError):reason="INVALID_JSON"
        except Exception:reason="SMALL_MODEL_ERROR"
    if reason:
        answer,large_ms=call(LARGE_MODEL,LARGE_PROMPT,text,1000); actual="LARGE"; final=LARGE_MODEL
    else: actual="SMALL"; final=SMALL_MODEL
    return {**case,"actualRoute":actual,"smallModelConfidence":confidence,"escalated":actual=="LARGE","escalationReason":reason,"finalModel":final,"smallModelLatencyMs":small_ms,"largeModelLatencyMs":large_ms,"totalLatencyMs":round((time.perf_counter()-total)*1000),"success":actual==case["expectedRoute"],"answerPreview":answer[:120]}

def avg(rows,key):
    values=[r[key] for r in rows if r[key] is not None]; return round(statistics.mean(values)) if values else None

def main():
    cases=[json.loads(line) for line in (ROOT/"data/test-cases.jsonl").read_text(encoding="utf-8-sig").splitlines() if line.strip()]
    rows=[route(c) for c in cases]; reasons=Counter(r["escalationReason"] for r in rows if r["escalationReason"])
    accuracy=sum(r["success"] for r in rows)
    lines=["# Model routing report","",f"Small model: {SMALL_MODEL}",f"Large model: {LARGE_MODEL}",f"Confidence threshold: {CONFIDENCE_THRESHOLD:.2f}","",f"Examples: {len(rows)}",f"Stayed on small model: {sum(not r['escalated'] for r in rows)}",f"Escalated to large model: {sum(r['escalated'] for r in rows)}",f"Routing accuracy: {accuracy}/{len(rows)}","","Escalation reasons:"]
    for reason in ("COMPLEX_REQUEST","LONG_REQUEST","LOW_CONFIDENCE","MODEL_REQUESTED_ESCALATION","ANSWER_TOO_SHORT","INVALID_JSON","SMALL_MODEL_ERROR"): lines.append(f"{reason}: {reasons[reason]}")
    lines += ["",f"Average small model latency: {avg(rows,'smallModelLatencyMs')} ms",f"Average large model latency: {avg(rows,'largeModelLatencyMs')} ms",f"Average total latency: {avg(rows,'totalLatencyMs')} ms","","| id | expected | actual | confidence | reason | final model | latency |","|---|---|---|---:|---|---|---:|"]
    for r in rows: lines.append(f"| {r['id']} | {r['expectedRoute']} | {r['actualRoute']} | {r['smallModelConfidence'] if r['smallModelConfidence'] is not None else '-'} | {r['escalationReason'] or '-'} | {r['finalModel']} | {r['totalLatencyMs']} ms |")
    report="\n".join(lines)+"\n"; print(report)
    (ROOT/"reports/latest.md").write_text(report,encoding="utf-8"); (ROOT/"reports/latest.json").write_text(json.dumps(rows,ensure_ascii=False,indent=2),encoding="utf-8")
if __name__=="__main__":
    try: main()
    except Exception as error: print(f"Routing runner failed: {error}",file=sys.stderr); sys.exit(1)
