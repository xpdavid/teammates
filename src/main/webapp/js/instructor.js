/*
 * This JavaScript file is included in all instructor pages. Functions here
 * should be common to some/all instructor pages.
 */

// Initial load-up
// -----------------------------------------------------------------------------

$(document).ready(function() {
    
    // bind the show picture onclick events
    bindStudentPhotoLink('.profile-pic-icon-click > .student-profile-pic-view-link');
    
    // bind the show picture onhover events
    bindStudentPhotoHoverLink('.profile-pic-icon-hover');

    // bind the event handler to show confirmation modal
    bindCourseDeleteLinks();
    bindSessionDeleteLinks();
});

// -----------------------------------------------------------------------------

/**
 * Checks whether a team's name is valid
 * Used in instructorCourseEnroll page (through instructorCourseEnroll.js)
 * @param teamName
 * @returns {Boolean}
 */
function isStudentTeamNameValid(teamName) {
    return teamName.length <= TEAMNAME_MAX_LENGTH;
}

/**
 * To check whether a student's name and team name are valid
 * @param editName
 * @param editTeamName
 * @returns {Boolean}
 */
function isStudentInputValid(editName, editTeamName, editEmail) {
    if (editName === '' || editTeamName === '' || editEmail === '') {
        setStatusMessage(DISPLAY_FIELDS_EMPTY, StatusType.DANGER);
        return false;
    } else if (!isNameValid(editName)) {
        setStatusMessage(DISPLAY_NAME_INVALID, StatusType.DANGER);
        return false;
    } else if (!isStudentTeamNameValid(editTeamName)) {
        setStatusMessage(DISPLAY_STUDENT_TEAMNAME_INVALID, StatusType.DANGER);
        return false;
    } else if (!isEmailValid(editEmail)) {
        setStatusMessage(DISPLAY_EMAIL_INVALID, StatusType.DANGER);
        return false;
    }
    
    return true;
}

function setupFsCopyModal() {
    $('#fsCopyModal').on('show.bs.modal', function(event) {
        var button = $(event.relatedTarget); // Button that triggered the modal
        var actionlink = button.data('actionlink');
        var courseid = button.data('courseid');
        var fsname = button.data('fsname');
        var appUrl = window.location.origin;
        var currentPage = window.location.href.substring(appUrl.length); // use the page's relative URL
        
        $.ajax({
            type: 'GET',
            url: actionlink + '&courseid=' + encodeURIComponent(courseid) + '&fsname=' + encodeURIComponent(fsname)
                 + '&currentPage=' + encodeURIComponent(currentPage),
            beforeSend: function() {
                $('#fscopy_submit').prop('disabled', true);
                $('#courseList').html('Loading possible destination courses. Please wait ...<br>'
                                      + "<img class='margin-center-horizontal' src='/images/ajax-loader.gif'/>");
            },
            error: function() {
                $('#courseList').html("<p id='fs-copy-modal-error'>Error retrieving course list."
                                      + 'Please close the dialog window and try again.</p>');
            },
            success: function(data) {
                $('#courseList').html(data);
                // If the user alt-clicks, the form does not send any parameters and results in an error.
                // Prevent default form submission and submit using jquery.
                $('#fscopy_submit').off('click')
                                   .on('click',
                                        function(event) {
                                            $('#fscopy_submit').prop('disabled', true);
                                            event.preventDefault();
                                            $('#fscopy_submit').closest('form').submit();
                                        }
                                    );
                $('#fscopy_submit').prop('disabled', false);
            }
        });
    });

    $('#instructorCopyModalForm').submit(
        function(e) {
            e.preventDefault();
            var $this = $(this);
            
            var $copyModalStatusMessage = $('#feedback-copy-modal-status');
            
            $.ajax({
                type: 'POST',
                url: $this.prop('action'),
                data: $this.serialize(),
                beforeSend: function() {
                    $copyModalStatusMessage.removeClass('alert alert-danger');
                    $copyModalStatusMessage.html('<img src="/images/ajax-loader.gif" class="margin-center-horizontal">');
                },
                error: function() {
                    $copyModalStatusMessage.addClass('alert alert-danger');
                    $copyModalStatusMessage.text('There was an error during submission. '
                                                 + 'Please close the dialog window and try again.');
                },
                success: function(data) {
                    var isError = data.errorMessage !== '';
                    if (!isError && data.redirectUrl) {
                        window.location.href = data.redirectUrl;
                    } else {
                        $copyModalStatusMessage.addClass('alert alert-danger');
                        $copyModalStatusMessage.text(data.errorMessage);
                        $('#fscopy_submit').prop('disabled', false);
                    }
                }
            });
        }
    );
}

// Student Profile Picture
// --------------------------------------------------------------------------

/**
 * @param elements:
 * identifier that points to elements with
 * class: student-profile-pic-view-link
 */
function bindStudentPhotoLink(elements) {
    $(elements).on('click', function(e) {
        var event = e || window.event;
        
        event.cancelBubble = true;
        
        if (event.stopPropagation) {
            event.stopPropagation();
        }
        
        var actualLink = $(this).parent().attr('data-link');
        var $loadingPlaceholder = $('<img>')
                                  .attr('src', '/images/ajax-loader.gif')
                                  .addClass('profile-pic-icon-click-ajax-loading-img');
        
        $(this).siblings('img').attr('src', actualLink).load(function() {
            var actualLink = $(this).parent().attr('data-link');
            var resolvedLink = $(this).attr('src');
            
            $loadingPlaceholder.remove();
            
            $(this).removeClass('hidden')
                .parent().attr('data-link', '')
                .popover({
                    html: true,
                    trigger: 'manual',
                    placement: 'top',
                    content: function() {
                        return '<img class="profile-pic" src="' + resolvedLink + '">';
                    }
                })
                .mouseenter(function() {
                    $(this).popover('show');
                    $(this).siblings('.popover').on('mouseleave', function() {
                        $(this).siblings('.profile-pic-icon-click').popover('hide');
                    });
                    $(this).mouseleave(function() {
                        // this is so that the user can hover over the
                        // pop-over photo without hiding the photo
                        setTimeout(function(obj) {
                            if ($(obj).siblings('.popover').find(':hover').length === 0) {
                                $(obj).popover('hide');
                            }
                        }, 200, this);
                    });
                });
            
            updateHoverShowPictureEvents(actualLink, resolvedLink);
        });
        
        var $parentDiv = $(this).parent();
        $(this).remove();
        $parentDiv.append($loadingPlaceholder);
        
    });
}

/**
 * @param elements:
 * identifier that points to elements with
 * class: profile-pic-icon-hover
 */
function bindStudentPhotoHoverLink(elements) {
    $(elements)
        .mouseenter(function() {
            $(this).popover('show');
            $(this).siblings('.popover').on('mouseleave', function() {
                $(this).siblings('.profile-pic-icon-hover').popover('hide');
            });
        })
        .mouseleave(function() {
            // this is so that the user can hover over the
            // pop-over without accidentally hiding the 'view photo' link
            setTimeout(function(obj) {
                if ($(obj).siblings('.popover').find('.profile-pic').length !== 0
                    || $(obj).siblings('.popover').find(':hover').length === 0) {

                    $(obj).popover('hide');
                }
            }, 200, this);
        });
    
    // bind the default popover event for the
    // show picture onhover events
    $(elements).popover({
        html: true,
        trigger: 'manual',
        placement: 'top',
        content: function() {
            return '<a class="cursor-pointer" onclick="'
                   + 'loadProfilePictureForHoverEvent($(this).closest(\'.popover\').siblings(\'.profile-pic-icon-hover\'))">'
                   + 'View Photo</a>';
        }
    });
}

function bindDeleteButtons() {
    $('body').on('click', '.session-delete-for-test', function(event) {
        event.preventDefault();

        var $button = $(event.target);
        var courseId = $button.data('courseid');
        var feedbackSessionName = $button.data('fsname');

        var messageText = 'Are you want to delete the feedback session ' + feedbackSessionName + ' in ' + courseId + '?';
        var okCallback = function() {
            window.location = $button.attr('href');
        };

        BootboxWrapper.showModalConfirmation('Confirm deleting feedback session', messageText, okCallback, null,
                BootboxWrapper.DEFAULT_OK_TEXT, BootboxWrapper.DEFAULT_CANCEL_TEXT, StatusType.DANGER);
    });
}

function bindCourseDeleteLinks() {
    $('body').on('click', '.course-delete-link', function(event) {
        event.preventDefault();

        var $clickedLink = $(event.target);
        var messageText = 'Are you sure you want to delete the course: ' + $clickedLink.data('courseId') + '? '
                          + 'This operation will delete all students and sessions in this course. '
                          + 'All instructors of this course will not be able to access it hereafter as well.';
        var okCallback = function() {
            window.location = $clickedLink.attr('href');
        };

        BootboxWrapper.showModalConfirmation('Confirm deleting course', messageText, okCallback, null,
                BootboxWrapper.DEFAULT_OK_TEXT, BootboxWrapper.DEFAULT_CANCEL_TEXT, StatusType.DANGER);
    });
}

function bindSessionDeleteLinks() {
    $('body').on('click', '#fsDeleteLink', function(event) {
        event.preventDefault();

        var $clickedLink = $(event.target);
        var messageText = 'Are you sure you want to delete the feedback session ' + $clickedLink.data('feedbackSessionName')
                          + ' in ' + $clickedLink.data('courseId') + '?';
        var okCallback = function() {
            window.location = $clickedLink.attr('href');
        };

        BootboxWrapper.showModalConfirmation('Confirm deleting feedback session', messageText, okCallback, null,
                BootboxWrapper.DEFAULT_OK_TEXT, BootboxWrapper.DEFAULT_CANCEL_TEXT, StatusType.DANGER);
    });
}

function attachEventToDeleteStudentLink() {
    $(document).on('click', '.course-student-delete-link', function(event) {
        event.preventDefault();

        var $clickedLink = $(event.target);
        var messageText = 'Are you sure you want to remove ' + $clickedLink.data('studentName')
                          + ' from the course ' + $clickedLink.data('courseId') + '?';
        var okCallback = function() {
            window.location = $clickedLink.attr('href');
        };

        BootboxWrapper.showModalConfirmation('Confirm deletion', messageText, okCallback, null,
                BootboxWrapper.DEFAULT_OK_TEXT, BootboxWrapper.DEFAULT_CANCEL_TEXT, StatusType.DANGER);
    });
}

function bindRemindButtons() {
    $('body').on('click', '.session-remind-inner-for-test, .session-remind-for-test', function(event) {
        event.preventDefault();

        var $button = $(event.target);
        var messageText = 'Send e-mails to remind students who have not submitted their feedback for '
                          + $button.data('fsname') + '?';
        var okCallback = function() {
            window.location = $button.attr('href');
        };

        BootboxWrapper.showModalConfirmation('Confirm sending reminders', messageText, okCallback, null,
                BootboxWrapper.DEFAULT_OK_TEXT, BootboxWrapper.DEFAULT_CANCEL_TEXT, StatusType.INFO);
    });
}

function bindPublishButtons() {
    $('body').on('click', '.session-publish-for-test', function(event) {
        event.preventDefault();
 
        var $button = $(this);
        var feedbackSessionName = $button.data('fsname');
        var messageText = 'Are you sure you want to publish the responses for the session "' + feedbackSessionName + '"?';

        var isSendingPublishedEmail = $button.data('sending-published-email');
        if (isSendingPublishedEmail) {
            messageText += ' An email will be sent to students to inform them that the responses are ready for viewing.';
        }

        var okCallback = function() {
            window.location = $button.attr('href');
        };

        BootboxWrapper.showModalConfirmation('Confirm publishing responses', messageText, okCallback, null,
                BootboxWrapper.DEFAULT_OK_TEXT, BootboxWrapper.DEFAULT_CANCEL_TEXT, StatusType.WARNING);
    });
}

function bindUnpublishButtons() {
    $('body').on('click', '.session-unpublish-for-test', function(event) {
        event.preventDefault();

        var $button = $(event.target);
        var messageText = 'Are you sure you want to unpublish the session ' + $button.data('fsname') + '?';
        var okCallback = function() {
            window.location = $button.attr('href');
        };

        BootboxWrapper.showModalConfirmation('Confirm unpublishing responses', messageText, okCallback, null,
                BootboxWrapper.DEFAULT_OK_TEXT, BootboxWrapper.DEFAULT_CANCEL_TEXT, StatusType.WARNING);
    });
}

/**
 * completes the loading cycle for showing profile picture
 * for a onhover event
 * @param link
 * @param resolvedLink
 */
function loadProfilePictureForHoverEvent(obj) {
    obj.children('img')[0].src = obj.attr('data-link');
    
    // load the pictures in all similar links
    obj.children('img').load(function() {
        var actualLink = $(this).parent().attr('data-link');
        var resolvedLink = $(this).attr('src');

        updateHoverShowPictureEvents(actualLink, resolvedLink);
        
        // this is to show the picture immediately for the one
        // the user just clicked on
        $(this).parent()
            .popover('show')
            // this is to handle the manual hide action of the popover
            .siblings('.popover').on('mouseleave', function() {
                $(this).siblings('.profile-pic-icon-hover').popover('hide');
            });
    });
}

/**
 * updates all the student names that show profile picture
 * on hover with the resolved link after one instance of the name
 * has been loaded<br>
 * Helps to avoid clicking view photo when hovering over names of
 * students whose picture has already been loaded elsewhere in the page
 * @param link
 * @param resolvedLink
 */
function updateHoverShowPictureEvents(actualLink, resolvedLink) {
    $('.profile-pic-icon-hover[data-link="' + actualLink + '"]')
        .attr('data-link', '')
        .off('mouseenter mouseleave')
        .popover('destroy')
        .popover({
            html: true,
            trigger: 'manual',
            placement: 'top',
            content: function() {
                return '<img class="profile-pic" src="' + resolvedLink + '">';
            }
        })
        .mouseenter(function() {
            $(this).popover('show');
            $(this).siblings('.popover').on('mouseleave', function() {
                $(this).siblings('.profile-pic-icon-hover').popover('hide');
            });
            $(this).mouseleave(function() {
                // this is so that the user can hover over the
                // pop-over photo without hiding the photo
                setTimeout(function(obj) {
                    if ($(obj).siblings('.popover').find(':hover').length === 0) {
                        $(obj).popover('hide');
                    }
                }, 200, this);
            });
        })
        .children('img[src=""]').attr('src', resolvedLink);
}

// --------------------------------------------------------------------------
