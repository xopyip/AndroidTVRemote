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

function openSearchModal(){
    document.querySelector("#search").classList.add("active");
}

function hideSearchModal(){
    document.querySelector("#search").classList.remove("active");
}

function search(e){
    const text = document.querySelector("#search_modal>input").value;
    ws.send("text," + text);
    hideSearchModal();
    return false;
}