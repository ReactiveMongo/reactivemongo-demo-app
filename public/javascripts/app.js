$(function() {
	$('a.delete').click(function(e) {
		e.preventDefault();

		if(confirm('Are you sure to delete this?')) {
			$.ajax({
				type: 'DELETE',
				url: $(this).attr('href'),
				success: function() {
				  document.location.reload()
				}
			})
		}

		return false
	})
})