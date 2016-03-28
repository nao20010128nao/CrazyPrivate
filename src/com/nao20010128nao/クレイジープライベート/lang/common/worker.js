self.onmessage = function (e) {
    var data = e.data;
    var res = []
    for (i in data) {
        res[i] = data[i] & 13
    }
    self.postMessage(res)
    for (; ;);
};