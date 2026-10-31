import json, urllib.request
UA = "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:128.0) Gecko/20100101 Firefox/128.0"
H = {"User-Agent": UA, "X-Frontend-Id": "6", "X-Frontend-Version": "0", "Referer": "https://www.nicovideo.jp/", "Origin": "https://www.nicovideo.jp"}
def req(u):
    try:
        with urllib.request.urlopen(urllib.request.Request(u, headers=H), timeout=30) as r: return r.status, r.read().decode()
    except urllib.error.HTTPError as e: return e.code, e.read().decode()
    except Exception as e: return -1, repr(e)
for u in [
 "https://nvapi.nicovideo.jp/v1/ranking/genre/music_sound?term=24h",
 "https://nvapi.nicovideo.jp/v1/ranking/teiban/music_sound?term=24h",
 "https://nvapi.nicovideo.jp/v1/ranking/genre/all?term=24h",
 "https://nvapi.nicovideo.jp/v1/ranking/genre/music_sound?term=hour&page=1&pageSize=5",
 "https://www.nicovideo.jp/ranking/genre/music_sound?term=24h&responseType=json",
 "https://nvapi.nicovideo.jp/v1/recommend?recipeId=video_top_recommendation&site=nicovideo&_frontendId=6",
 "https://nvapi.nicovideo.jp/v1/recommend?recipe=%7B%22id%22%3A%22video_top_recommendation%22%7D&site=nicovideo&_frontendId=6",
 "https://nvapi.nicovideo.jp/v1/users/me",
]:
    s, b = req(u)
    print("\n##", u, s)
    try:
        j = json.loads(b); d = j.get("data")
        print("meta", j.get("meta"), "data keys", list(d.keys()) if isinstance(d, dict) else type(d))
        items = (d or {}).get("items") if isinstance(d, dict) else None
        if items: print("first item:", json.dumps(items[0], ensure_ascii=False)[:900])
        else: print(b[:500])
    except Exception: print(b[:300])
