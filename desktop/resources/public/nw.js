var resizeGrid = function() {
	var resize = function() {
		var minTileWidth = 160;
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

var importImage = function(elem) {
	var reader = new FileReader();

	reader.onload = function(event) {
		var url = 'url(' + event.target.result + ')';
		$('.profile-image').css('background-image', url);
	};

	reader.readAsDataURL(elem.files[0]);
};

$(document).foundation();
$(window).resize(resizeGrid);
resizeGrid();
