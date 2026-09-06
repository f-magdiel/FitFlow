from fastapi import FastAPI, HTTPException
from fastapi.responses import HTMLResponse
from typing import Any

from src import server as mcp_server

app = FastAPI(title="MCP Server Bridge")


@app.get("/tools")
def list_tools() -> Any:
        # Minimal static listing for this example
        return {"tools": ["add"], "resources": ["greeting://{name}"]}


@app.get("/run/add")
def run_add(a: int, b: int) -> Any:
        try:
                result = mcp_server.add(a, b)
                return {"result": result}
        except Exception as e:
                raise HTTPException(status_code=500, detail=str(e))


@app.get("/resource/greeting/{name}")
def get_greeting(name: str) -> Any:
        try:
                return {"greeting": mcp_server.get_greeting(name)}
        except Exception as e:
                raise HTTPException(status_code=500, detail=str(e))


@app.get("/inspector", response_class=HTMLResponse)
def inspector_ui() -> HTMLResponse:
        html = """
        <!doctype html>
        <html>
            <head>
                <meta charset="utf-8" />
                <title>MCP Bridge Inspector</title>
                <style>body{font-family:Arial,Helvetica,sans-serif;margin:20px;}input{margin:4px}</style>
            </head>
            <body>
                <h2>MCP Bridge Inspector</h2>
                <div id="tools"></div>
                <hr/>
                <div>
                    <h3>Run `add`</h3>
                    <input id="a" type="number" value="1"/> + <input id="b" type="number" value="2"/>
                    <button onclick="runAdd()">Run</button>
                    <pre id="addResult"></pre>
                </div>
                <hr/>
                <div>
                    <h3>Greeting Resource</h3>
                    <input id="gname" type="text" value="World"/>
                    <button onclick="getGreeting()">Get</button>
                    <pre id="greetResult"></pre>
                </div>
                <script>
                    async function loadTools(){
                        const res = await fetch('/tools');
                        const j = await res.json();
                        document.getElementById('tools').innerText = JSON.stringify(j, null, 2);
                    }
                    async function runAdd(){
                        const a = document.getElementById('a').value;
                        const b = document.getElementById('b').value;
                        const res = await fetch(`/run/add?a=${a}&b=${b}`);
                        const j = await res.json();
                        document.getElementById('addResult').innerText = JSON.stringify(j, null, 2);
                    }
                    async function getGreeting(){
                        const name = encodeURIComponent(document.getElementById('gname').value);
                        const res = await fetch(`/resource/greeting/${name}`);
                        const j = await res.json();
                        document.getElementById('greetResult').innerText = JSON.stringify(j, null, 2);
                    }
                    loadTools();
                </script>
            </body>
        </html>
        """
        return HTMLResponse(content=html, status_code=200)
