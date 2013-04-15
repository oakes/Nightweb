$(document).foundation();

var minTileWidth = 160;
var resizeGrid = function() {
	var resize = function() {
		var totalWidth = document.documentElement.clientWidth;
		var numColumns = Math.floor(totalWidth / minTileWidth);
		var newTileWidth = totalWidth / numColumns;
		$('.grid-view li').
			css('width', newTileWidth).
			css('height', newTileWidth);
	};
	resize();
	resize();
};

$(window).resize(resizeGrid);
resizeGrid();
