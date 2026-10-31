import json, random, string, time, re, urllib.request, urllib.parse, http.cookiejar

UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
cj = http.cookiejar.CookieJar()
op = urllib.request.build_opener(urllib.request.HTTPCookieProcessor(cj))

def req(url, data=None, headers=None, method=None):
    h = {"User-Agent": UA}
    h.update(headers or {})
    r = urllib.request.Request(url, data=data, headers=h, method=method)
    try:
        with op.open(r, timeout=30) as resp:
            return resp.status, resp.read().decode("utf-8", "replace"), dict(resp.headers)
    except urllib.error.HTTPError as e:
        return e.code, e.read().decode("utf-8", "replace"), dict(e.headers)
    except Exception as e:
        return -1, repr(e), {}

def section(t): print("\n==== " + t + " ====", flush=True)


section("Google Fonts")
s, css, _ = req("https://fonts.googleapis.com/css2?family=Noto+Sans+JP:wght@100..900", headers={"User-Agent": "curl/8"})
print(s, css[:600])

section("niconico watch variants")
api_h = {"X-Frontend-Id": "6", "X-Frontend-Version": "0", "X-Niconico-Language": "ja-jp",
         "Referer": "https://www.nicovideo.jp/", "Origin": "https://www.nicovideo.jp"}
tid = "".join(random.choices(string.ascii_letters + string.digits, k=10)) + "_" + str(int(time.time() * 1000))
variants = [
  ("json-noheaders", "https://www.nicovideo.jp/watch/sm9?responseType=json", {}),
  ("json-headers", "https://www.nicovideo.jp/watch/sm9?responseType=json", api_h),
  ("json-track", f"https://www.nicovideo.jp/watch/sm9?responseType=json&actionTrackId={tid}", {}),
  ("v3_guest", f"https://www.nicovideo.jp/api/watch/v3_guest/sm9?_frontendId=6&_frontendVersion=0&actionTrackId={tid}", api_h),
  ("v3_guest-noheaders", f"https://www.nicovideo.jp/api/watch/v3_guest/sm9?_frontendId=6&_frontendVersion=0&actionTrackId={tid}", {}),
  ("html", "https://www.nicovideo.jp/watch/sm9", {}),
]
for name, url, h in variants:
    s, b, hd = req(url, headers=h)
    print("\n--", name, s, "len", len(b))
    if name == "html":
        m = re.search(r'name="server-response" content="([^"]+)"', b)
        print("server-response meta:", bool(m))
        if m:
            import html as H
            j = json.loads(H.unescape(m.group(1)))
            resp = j.get("data", {}).get("response", {})
            print("keys", list(resp.keys())[:30])
            dom = (resp.get("media") or {}).get("domand")
            print("domand", json.dumps(dom, ensure_ascii=False)[:1200] if dom else None)
            print("client", json.dumps(resp.get("client"), ensure_ascii=False)[:300])
            if dom:
                vids = [v for v in dom["videos"] if v.get("isAvailable")]
                auds = [a for a in dom["audios"] if a.get("isAvailable")]
                track = (resp.get("client") or {}).get("watchTrackId") or tid
                for label, outputs in [("lowest+best", [[min(vids, key=lambda v: v.get("bitRate", 0))["id"], max(auds, key=lambda a: a.get("bitRate", 0))["id"]]]),
                                       ("all", [[v["id"], a["id"]] for v in vids for a in auds])]:
                    body = json.dumps({"outputs": outputs}).encode()
                    hh = dict(api_h); hh.update({"X-Access-Right-Key": dom["accessRightKey"], "X-Request-With": "https://www.nicovideo.jp", "Content-Type": "application/json"})
                    s2, b2, hd2 = req(f"https://nvapi.nicovideo.jp/v1/watch/sm9/access-rights/hls?actionTrackId={track}", data=body, headers=hh, method="POST")
                    print("hls", label, s2, b2[:400], "| set-cookie:", hd2.get("Set-Cookie", "")[:160])
                    if s2 in (200, 201):
                        url2 = json.loads(b2)["data"]["contentUrl"]
                        s3, b3, _ = req(url2)
                        print("master", s3, b3[:900])
                        am = re.search(r'URI="([^"]+)"', b3)
                        if am:
                            s4, b4, _ = req(urllib.parse.urljoin(url2, am.group(1)))
                            print("audio playlist", s4, b4[:500])
    else:
        try:
            j = json.loads(b); print("meta", j.get("meta"), "data keys", list((j.get("data") or {}).keys())[:20])
            r = (j.get("data") or {}).get("response") or j.get("data") or {}
            print("has media.domand:", bool((r.get("media") or {}).get("domand")))
        except Exception as e:
            print("not json:", b[:200])

section("nvapi search variants")
for params in [{"keyword": "千本桜", "sortKey": "hot", "sortOrder": "none", "pageSize": 2, "page": 1},
               {"keyword": "千本桜", "sortKey": "viewCount", "sortOrder": "desc", "pageSize": 2, "page": 1, "genres": "music_sound"},
               {"keyword": "千本桜", "pageSize": 2, "page": 1}]:
    s, b, _ = req("https://nvapi.nicovideo.jp/v2/search/video?" + urllib.parse.urlencode(params), headers={"X-Frontend-Id": "6", "X-Frontend-Version": "0"})
    try:
        j = json.loads(b); print(params, s, j.get("meta"), [ (i["id"], i["title"][:20], i.get("owner",{}).get("name")) for i in j.get("data",{}).get("items",[])])
    except Exception: print(params, s, b[:300])
