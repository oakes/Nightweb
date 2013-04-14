$(document).foundation();

var minTileWidth = 160;
var resizeGrid = function() {
	var totalWidth = $(window).width();
	var numColumns = Math.floor(totalWidth / minTileWidth);
	var newTileWidth = totalWidth / numColumns;
	$('.grid-view li').
		css('width', newTileWidth).
		css('height', newTileWidth);
};

$(window).resize(resizeGrid);
resizeGrid();
