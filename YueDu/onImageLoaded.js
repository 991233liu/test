function onImageLoaded(e, aid) {
  if (!aid) {
    aid = aid = (aid = (aid = e.id.split("."))[0].split("_"))[2]
  }
var t = document.createElement("canvas");
e.after(t)
	var a = t.getContext("2d"),
		n = e.width,
		d = e.naturalWidth,
		i = e.naturalHeight;
	t.width = d, t.height = i, t.style.width = "100%", t.style.display = "block";
	var o = document.getElementById(e.id)
		.parentNode;
	o = (o = (o = e.id.split("."))[0].split("_"))[2]
	for (var s = get_num(window.btoa(aid), window.btoa(o)), l = parseInt(i % s), r = d, m = 0; m < s; m++) {
		var c = Math.floor(i / s),
			g = c * m,
			w = i - c * (m + 1) - l;
		0 == m ? c += l : g += l, a.drawImage(e, 0, w, r, c, 0, g, r, c)
	}
	e.style.display = "none";
}

function get_num(e, t) {
	var a = 10,
		n = (e = window.atob(e)) + (t = window.atob(t));
	switch (n = (n = (n = md5(n))
			.substr(-1))
		.charCodeAt(), e >= window.atob("MjY4ODUw") && e <= window.atob("NDIxOTI1") ? n %= 10 : e >= window.atob("NDIxOTI2") && (n %= 8), n) {
		case 0:
			a = 2;
			break;
		case 1:
			a = 4;
			break;
		case 2:
			a = 6;
			break;
		case 3:
			a = 8;
			break;
		case 4:
			a = 10;
			break;
		case 5:
			a = 12;
			break;
		case 6:
			a = 14;
			break;
		case 7:
			a = 16;
			break;
		case 8:
			a = 18;
			break;
		case 9:
			a = 20
	}
	return a
}
