function logIn() {
  $.ajax({
    url: Base.baseUrl + "/api/user/log_in",
    type: "POST",
    async: false,
    contentType: "application/json",
    data: JSON.stringify({
      username: $("#username").val(),
      password: $("#password").val(),
    }),
    dataType: "json",
    success: function (result) {
      if (
        result == null ||
        result.status == null ||
        result.status.http_status == null
      ) {
        alert("Something went wrong: " + JSON.stringify(result));
        return;
      }

      if (result.status.http_status != "OK") {
        alert("Login failed: " + result.status.exception_code);
        return;
      }

      logInSuccess(result);
      return result;
    },
  });
}

function logInSuccess(result) {
  if (
    result == null ||
    result.payload == null ||
    result.payload.id == null ||
    result.payload.token == null
  ) {
    alert("Something went wrong");
    return;
  }

  // Base.setCookie("user", JSON.stringify(result.payload), 60*24);
  alert("Login successfully!");
  window.location.href = Base.originUrl + "/dashboard/index.html";
}

var popupWindow = null;
const popupCenter = ({ url, title, w, h }) => {
  // Fixes dual-screen position                             Most browsers      Firefox
  const dualScreenLeft =
    window.screenLeft !== undefined ? window.screenLeft : window.screenX;
  const dualScreenTop =
    window.screenTop !== undefined ? window.screenTop : window.screenY;

  const width = window.innerWidth
    ? window.innerWidth
    : document.documentElement.clientWidth
    ? document.documentElement.clientWidth
    : screen.width;
  const height = window.innerHeight
    ? window.innerHeight
    : document.documentElement.clientHeight
    ? document.documentElement.clientHeight
    : screen.height;

  const systemZoom = width / window.screen.availWidth;
  const left = (width - w) / 2 / systemZoom + dualScreenLeft;
  const top = (height - h) / 2 / systemZoom + dualScreenTop;
  popupWindow = window.open(
    url,
    title,
    `
      scrollbars=yes,
      width=${w / systemZoom}, 
      height=${h / systemZoom}, 
      top=${top}, 
      left=${left}
      `
  );

  if (window.focus) popupWindow.focus();
};

function logInWithChatbot() {
  connectSocket();
  // popupCenter({url: Base.originUrl + "/log_in_from_chatbot/", title: 'Chatbot - Login', w: 570, h: 520});
}

var socketConnected = false;
function connectSocket() {
  if (socketConnected) return;
  var socket = new SockJS(Base.baseUrl + "/api/ws_endpoint");
  stompClient = Stomp.over(socket);
  stompClient.connect({}, function (frame) {
    console.log("Connected: " + frame);
    socketConnected = true;

    // stompClient.subscribe("/chat-listener/652a98842a247b032b3d0707", function (result) {
    stompClient.subscribe("/chat/169535484916/receive-from-bot", function (result) {
      console.log(result);
      // socketListener(result);
    });

//    stompClient.subscribe("/user/" + frame.headers['user-name'] + "/queue/greetings", function (result) {
//      console.log(result);
//      socketListener(result);
//    });
  });
}

function socketListener(result) {
  console.log(result);
//  if (result == null || result.body == null) {
//    alert("Log in fail!");
//    return;
//  }
//
//  var message = JSON.parse(result.body);
//  if (message.status == null || message.user == null) {
//    alert("Something went wrong!");
//    return;
//  }
//
//  if (message.status.http_status != "OK") {
//    alert(message.status.exception_code);
//    return;
//  }

  // Base.setCookie("user", JSON.stringify(message.user), 60*24);
//  window.location.href = Base.originUrl + "/dashboard/index.html";
}
