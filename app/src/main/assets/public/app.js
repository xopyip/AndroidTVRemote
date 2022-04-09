var exampleSocket = new WebSocket("ws://" + window.location.host + "/ws");
//todo: proper connection handling
function handleButton(str){
    exampleSocket.send("click," + str);
}