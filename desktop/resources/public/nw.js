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

var resizeImage = function(src, crop, callback) {
	var img = new Image();

	img.onload = function() {
		var canvas = document.createElement("canvas");
		var context = canvas.getContext("2d");

		var maxSize = 1024;
		var sx = 0, sy = 0;

		if (crop) {
			var size = Math.min(img.width, img.height, maxSize);
			var width = size, height = size;

			if (img.width > img.height) {
				sx = -1 * (img.width - img.height) / 2;
			} else {
				sy = -1 * (img.height - img.width) / 2;
			}
		} else {
			var width = Math.min(img.width, maxSize);
			var height = Math.min(img.height, maxSize);
		}

	        canvas.width = width;
	        canvas.height = height;
	        context.drawImage(img, sx, sy);

		callback(canvas.toDataURL("image/webp", 0.9));
	};

	img.src = src;
};

var importImage = function(elem) {
	var reader = new FileReader();

	reader.onload = function(event) {
		resizeImage(
			event.target.result,
			true,
			function(src) {
				$('#profile-image').css(
					'background-image',
					'url(' + src + ')'
				);
				$('#profile-image-hidden').val(src);
			}
		);
	};

	reader.readAsDataURL(elem.files[0]);
};

var saveProfile = function() {
	$.ajax({
		type: 'POST',
		url: '/',
		data: {
			type: 'profile',
			name: $('#profile-name').val(),
			body: $('#profile-about').val(),
			pic: $('#profile-image-hidden').val()
		},
		success: function(data) {
			window.location.reload(true);
		}
	});
};

$(document).foundation();
$(window).resize(resizeGrid);
resizeGrid();
