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

section("JitPack NewPipeExtractor")
for u in ["https://jitpack.io/api/builds/com.github.TeamNewPipe/NewPipeExtractor/latest",
          "https://jitpack.io/api/builds/com.github.TeamNewPipe/NewPipeExtractor/latestOk"]:
    s, b, _ = req(u); print(u, s, b[:300])
s, b, _ = req("https://jitpack.io/api/builds/com.github.TeamNewPipe/NewPipeExtractor")
print("builds", s, b[:3000])

section("Montedio fonts")
s, html, _ = req("https://www.montedioyamagata.jp/")
print("home", s, len(html))
for m in sorted(set(re.findall(r'(?:href|src)="([^"]+?(?:\.css|fonts?[^"]*|typekit[^"]*|typesquare[^"]*))"', html))): print("link", m)
for m in sorted(set(re.findall(r'font-family:[^;"}]+', html))): print("inline", m)
css_links = re.findall(r'href="([^"]+?\.css[^"]*)"', html)
for c in css_links[:15]:
    url = urllib.parse.urljoin("https://www.montedioyamagata.jp/", c)
    s, css, _ = req(url)
    fams = sorted(set(re.findall(r'font-family:\s*([^;}]+)', css)))
    faces = re.findall(r'@font-face\s*{[^}]+}', css)
    imports = re.findall(r'@import[^;]+;', css)
    print("CSS", url, s, "families:", fams[:20])
    for f in faces[:10]: print("  FACE", f[:400])
    for i in imports: print("  IMPORT", i)

section("niconico snapshot")
base = "https://snapshot.search.nicovideo.jp/api/v2/snapshot/video/contents/search"
common = [("q", "初音ミク"), ("targets", "title,tags,description"),
          ("fields", "contentId,title,thumbnailUrl,lengthSeconds,viewCounter,userId,channelId"),
          ("_sort", "-viewCounter"), ("_limit", "3"), ("_context", "MonteVideoHub")]
for name, extra in [("plain", []),
                    ("genre.keyword", [("filters[genre.keyword][0]", "音楽・サウンド"), ("filters[genre.keyword][1]", "音楽")]),
                    ("genre", [("filters[genre][0]", "音楽・サウンド")])]:
    s, b, _ = req(base + "?" + urllib.parse.urlencode(common + extra))
    print(name, s, b[:700])

section("niconico nvapi search")
for u in ["https://nvapi.nicovideo.jp/v2/search/video?" + urllib.parse.urlencode({"keyword": "初音ミク", "sortKey": "viewCount", "sortOrder": "desc", "pageSize": 3, "page": 1}),
          "https://www.nicovideo.jp/search/" + urllib.parse.quote("初音ミク") + "?responseType=json"]:
    s, b, _ = req(u, headers={"X-Frontend-Id": "6", "X-Frontend-Version": "0"})
    print(u, s, b[:1500])

section("niconico watch sm9")
api_h = {"X-Frontend-Id": "6", "X-Frontend-Version": "0", "X-Niconico-Language": "ja-jp",
         "Referer": "https://www.nicovideo.jp/", "Origin": "https://www.nicovideo.jp"}
tid = "".join(random.choices(string.ascii_letters + string.digits, k=10)) + "_" + str(int(time.time() * 1000))
for vid in ["sm9", "sm45000000"]:
    s, b, _ = req(f"https://www.nicovideo.jp/watch/{vid}?responseType=json&actionTrackId={tid}", headers=api_h)
    print(vid, "watch", s, b[:300] if s != 200 else "")
    if s != 200: continue
    j = json.loads(b)
    resp = j.get("data", {}).get("response", {})
    print("top keys", list(j.keys()), "data keys", list(j.get("data", {}).keys()), "response keys", list(resp.keys())[:40])
    dom = (resp.get("media") or {}).get("domand")
    print("domand", json.dumps(dom, ensure_ascii=False)[:1500] if dom else None)
    print("client", json.dumps(resp.get("client"), ensure_ascii=False)[:400])
    if not dom: continue
    vids = [v for v in dom["videos"] if v.get("isAvailable")]
    auds = [a for a in dom["audios"] if a.get("isAvailable")]
    track = (resp.get("client") or {}).get("watchTrackId") or tid
    for label, outputs in [("lowest+best", [[min(vids, key=lambda v: v.get("bitRate", 0))["id"], max(auds, key=lambda a: a.get("bitRate", 0))["id"]]]),
                           ("all", [[v["id"], a["id"]] for v in vids for a in auds])]:
        body = json.dumps({"outputs": outputs}).encode()
        h = dict(api_h); h.update({"X-Access-Right-Key": dom["accessRightKey"], "X-Request-With": "https://www.nicovideo.jp", "Content-Type": "application/json"})
        s2, b2, hd = req(f"https://nvapi.nicovideo.jp/v1/watch/{vid}/access-rights/hls?actionTrackId={track}", data=body, headers=h, method="POST")
        print("hls", label, s2, b2[:600], "set-cookie:", hd.get("Set-Cookie", "")[:200])
        if s2 == 201 or s2 == 200:
            url = json.loads(b2)["data"]["contentUrl"]
            s3, b3, _ = req(url)
            print("master", s3, b3[:800])
