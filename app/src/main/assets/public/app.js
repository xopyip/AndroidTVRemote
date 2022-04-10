var ws = null;
function connect(){
    ws = new WebSocket("ws://" + window.location.host + "/ws");

    ws.onclose = function(e) {
        setTimeout(function() {
          connect();
        }, 1000);
    };

    ws.onerror = function(err) {
        ws.close();
    };
}
connect();

function handleButton(str){
    if(ws == null){
        alert("WebSocket isn't connected");
        return;
    }
    ws.send("click," + str);
}