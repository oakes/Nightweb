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

var profilePicker = function(elem) {
	var reader = new FileReader();

	reader.onload = function(event) {
		resizeImage(
			event.target.result,
			true,
			function(src) {
				$('#profile-image').css(
					'background-image',
					['url(', src, ')'].join('')
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
			type: 'save-profile',
			name: $('#profile-name').val(),
			body: $('#profile-about').val(),
			pic: $('#profile-image-hidden').val()
		},
		success: function(response) {
			window.location.reload(true);
		}
	});
};

var importUser = function() {
	var reader = new FileReader();

	reader.onload = function(event) {
		$.ajax({
			type: 'POST',
		  	url: '/',
		  	data: {
				type: 'import-user',
		  		file: event.target.result,
		  		pass: $('#import-password').val()
			},
		  	success: function(response) {
				if (response.length > 0) {
					alert(response);
				} else {
					window.location = '/';
				}
			}
		});
	};

	reader.readAsDataURL($('#import-pick').get(0).files[0]);
};

var exportUser = function() {
	$.ajax({
		type: 'POST',
		url: '/',
		data: {
			type: 'export-user',
			pass: $('#export-password').val()
		},
		success: function(response) {
			if (response.length > 0) {
				window.location = response;
				$('#export-dialog')
					.foundation('reveal', 'close');
			}
		}
	});
};

var attachments = [];

var attachPicker = function(elem) {
	for (var i in elem.files) {
		var reader = new FileReader();

		reader.onload = function(event) {
			resizeImage(
				event.target.result,
				false,
				function(src) {
					attachments.push(src);
					$('#attach-count').html([
						'(', attachments.length, ')'
					].join(''));
				}
			);
		};

		reader.readAsDataURL(elem.files[i]);
	}
};

var newPost = function() {
	$.ajax({
		type: 'POST',
		url: '/',
		data: {
			type: 'new-post',
			body: $('#new-post-body').val(),
			pics: ['["', attachments.join('" "'), '"]'].join('')
		},
		success: function(response) {
			window.location.reload(true);
		}
	});
};

var clearPost = function() {
	$('#new-post-dialog').get(0).reset();
	attachments = [];
	$('#attach-count').html('(0)');
};

// initialize framework
$(document).foundation();

// initialize grid
$(window).resize(resizeGrid);
resizeGrid();

// show spinner
var spinner = new Spinner({
	lines: 7, // The number of lines to draw
	length: 20, // The length of each line
	width: 10, // The line thickness
	radius: 30, // The radius of the inner circle
	corners: 1, // Corner roundness (0..1)
	rotate: 0, // The rotation offset
	direction: 1, // 1: clockwise, -1: counterclockwise
	color: '#000', // #rgb or #rrggbb
	speed: 1, // Rounds per second
	trail: 60, // Afterglow percentage
	shadow: false, // Whether to render a shadow
	hwaccel: true, // Whether to use hardware acceleration
	className: 'spinner', // The CSS class to assign to the spinner
	zIndex: 2e9, // The z-index (defaults to 2000000000)
	top: 'auto', // Top position relative to parent in px
	left: 'auto' // Left position relative to parent in px
});
$('#lightbox').hide();
$(document)
	.on('ajaxStart', function() {
		$('#lightbox').show();
		spinner.spin($('body').get(0));
	})
	.on('ajaxStop', function() {
		$('#lightbox').hide();
		spinner.stop();
	});
