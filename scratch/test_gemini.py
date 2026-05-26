import urllib.request
import json
import os

def test_api():
    # Read the .env file
    api_key = None
    try:
        with open("../.env", "r") as f:
            for line in f:
                if "GEMINI_API_KEY" in line:
                    parts = line.strip().split("=")
                    if len(parts) >= 2:
                        api_key = parts[1].strip()
    except Exception as e:
        print(f"Error reading .env file: {e}")
        return

    if not api_key:
        print("API Key not found in .env file.")
        return

    print(f"Found API Key: {api_key[:8]}...{api_key[-4:] if len(api_key) > 8 else ''}")

    # Use gemini-2.5-flash as it is guaranteed to exist and fast for test
    url = f"https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={api_key}"
    data = {
        "contents": [
            {
                "parts": [
                    {"text": "Di 'Hola, la API de Gemini responde correctamente' en una sola linea."}
                ]
            }
        ]
    }
    
    req_data = json.dumps(data).encode("utf-8")
    req = urllib.request.Request(
        url,
        data=req_data,
        headers={"Content-Type": "application/json"},
        method="POST"
    )

    try:
        with urllib.request.urlopen(req) as response:
            res_body = response.read().decode("utf-8")
            res_json = json.loads(res_body)
            print("Status Code: 200 OK")
            print("Response:")
            print(json.dumps(res_json, indent=2, ensure_ascii=False))
    except urllib.error.HTTPError as e:
        print(f"HTTP Error: {e.code} {e.reason}")
        try:
            err_body = e.read().decode("utf-8")
            err_json = json.loads(err_body)
            print(json.dumps(err_json, indent=2, ensure_ascii=False))
        except Exception:
            pass
    except Exception as e:
        print(f"Network / Unknown Error: {e}")

if __name__ == "__main__":
    test_api()
