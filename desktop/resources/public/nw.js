$(document).foundation();

var minTileWidth = 160;
var resizeGrid = function() {
	var resize = function() {
		var totalWidth = document.documentElement.clientWidth;
		var numColumns = Math.floor(totalWidth / minTileWidth);
		var newTileWidth = totalWidth / numColumns;
		$('.grid-view-tile').
			css('width', newTileWidth).
			css('height', newTileWidth);
	};
	resize();
	resize();
};

$(window).resize(resizeGrid);
resizeGrid();

var tileAction = function(url) {
	var params = {};
	url.split("&").forEach(function(elem, i, arr) {
		var param = elem.split("=");
		if (param.length = 2) {
			params[param[0]] = param[1];
		}
	});

	switch (params.type) {
		case "custom-func":
			$('#' + params.subtype + '-dialog').
				foundation('reveal', 'open');
			break;
	}
};
