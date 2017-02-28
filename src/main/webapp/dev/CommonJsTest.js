QUnit.module('common.js');

/**
 * Warning: This test must be the first because it tests on the visibility
 * of the elements. Testing later may push the elements out of view.
 */
QUnit.test('isWithinView()', function(assert) {
    var testDiv = $('#visible');

    // Applies style to visible element and asserts whether it should be visible
    function assertWithStyle(style, condition) {
        testDiv.attr('style', 'position: absolute;' + style);

        var testString = 'Element with style ' + style + (condition ? ' is within view' : ' is not within view');
        assert.equal(isWithinView(testDiv), condition, testString);
    }
    assertWithStyle('top: 0%;', true);
    assertWithStyle('top: 100%;', false);
    assertWithStyle('bottom: 0%;', true);
    assertWithStyle('bottom: 100%;', false);
    assertWithStyle('top: 0%; left: 0%;', true);
    assertWithStyle('top: 0%; left: -100%;', false);
    assertWithStyle('top: 0%; right: -100%;', false);
    assertWithStyle('top: 0%; right: 0%;', true);
});

QUnit.test('isNumber(num)', function(assert) {
    assert.equal(isNumber('-0.001'), true, 'Negative double');
    assert.equal(isNumber('12.056'), true, 'Positive double');
    assert.equal(isNumber('100356'), true, 'Positive integer');
    assert.equal(isNumber('-237'), true, 'Negative integer');
    assert.equal(isNumber('ABCDE'), false, 'Letters');
    assert.equal(isNumber('$12.57'), false, 'With Dollar Sign');
    assert.equal(isNumber('12A5'), false, 'Letter in Numbers');
    assert.equal(isNumber('0'), true, 'zero');
    assert.equal(isNumber('   124    '), true, 'With Spacing');
    assert.equal(isNumber('   12   4    '), false, 'With Spacing between');
});

/**
 * Test the whether the passed object is an actual date
 * with an accepted format
 *
 * Allowed formats : http://dygraphs.com/date-formats.html
 *
 * TEAMMATES currently follows the RFC2822 / IETF date syntax
 * e.g. 02 Apr 2012, 23:59
 */
QUnit.test('isDate(date)', function(assert) {
    assert.equal(isDate('12432567'), false, 'Numbers');
    assert.equal(isDate('0/0/0'), true, '0/0/0 - valid date on Firefox, invalid on Chrome');
    assert.equal(isDate('12/2/13'), true, '12/2/13 - valid format');
    assert.equal(isDate('12/02/2013'), true, '12/02/2013 - valid format (mm/dd/yyyy)');
    assert.equal(isDate('28/12/2013'), true, '28/12/2013 - valid format (dd/mm/yyyy)');
    assert.equal(isDate('12/12/13'), true, '12/02/13 - valid format');
    assert.equal(isDate('2013-12-12'), true, '2013-12-12 - valid format');
    assert.equal(isDate('28-12-2013'), false, '28-12-2013 - invalid format (dd-mm-yyyy)');
    assert.equal(isDate('2013-12-28'), true, '2013-12-28 - valid format (yyyy-mm-dd)');
    assert.equal(isDate('01 03 2003'), true, '01 03 2003 - valid format');
    assert.equal(isDate('A1/B3/C003'), false, 'A1/B3/C003 - invalid date');
    assert.equal(isDate('Abcdef'), false, 'Invalid Date string');
    assert.equal(isDate('02 Apr 2012, 23:59'), true, 'Valid Date string with time');
    assert.equal(isDate('02 Apr 2012'), true, 'Valid Date string without time');
    assert.equal(isDate('    12/12/01'), true, 'With Spacing in front');
    assert.equal(isDate('12 12 01       '), true, 'With Spacing behind');
    assert.equal(isDate('            12-12-01       '), false, 'With Spacing, invalid on Firefox and valid on Chrome');
    assert.equal(isDate('a12-12-2001'), false, 'a12-12-2001 - not in proper format');
    assert.equal(isDate('    a      12 12 2001'), false, '    a      12 12 2001 - not in proper format');
    assert.equal(isDate('12/12/2001   a  '), false, '12/12/2001   a  - not in proper format');
});

QUnit.test('scrollToTop()', function(assert) {
    // N/A, trivial function
    assert.expect(0);
});

QUnit.test('sortBase(x, y)', function(assert) {
    assert.equal(sortBase('abc', 'abc'), 0, 'Same text');
    assert.equal(sortBase('ABC', 'abc'), -1, 'Bigger text');
    assert.equal(sortBase('abc', 'ABC'), 1, 'Smaller text');
    assert.equal(sortBase('abc', 'efg'), -1, 'Different text');
    assert.equal(sortBase('ABC', 'efg'), -1, 'Bigger text');
    assert.equal(sortBase('abc', 'EFG'), 1, 'Smaller text');
});

QUnit.test('sortNum(x, y)', function(assert) {
    assert.equal(sortNum('1', '2'), -1, 'x=1, y=2');
    assert.equal(sortNum('-10', '2'), -12, 'x=-10, y=2');
    assert.equal(sortNum('3', '-1'), 4, 'x=3, y=-1');
    assert.equal(sortNum('0.1', '0.1'), 0, 'x=0.1, y=0.1');
    assert.equal(sortNum('-0.1', '0.1'), -0.2, 'x=-0.1, y=0.1');
    assert.equal(sortNum('0.1', '-0.1'), 0.2, 'x=-0.1, y=-0.1');
});

QUnit.test('sortDate(x, y)', function(assert) {
    assert.equal(sortDate('25 April 1999', '23 April 1999'), 1, '25 April 1999 - 23 April 1999');
    assert.equal(sortDate('25 April 1999 2:00', '25 April 1999 1:59'), 1,
        '25 April 1999 2:00PM - 25 April 1999 1:59PM');
    assert.equal(sortDate('25 April 1999 2:00', '25 April 1999 2:00'), 0,
        '25 April 1999 2:00PM - 25 April 1999 2:00PM');
    assert.equal(sortDate('25 April 1999 2:00', '25 April 1999 2:01'), -1,
        '25 April 1999 2:00PM - 25 April 1999 2:01PM');
});

QUnit.test('getPointValue(s, ditchZero)', function(assert) {
    // getPointValue() is used by the application itself, thus
    // the inputs are always valid.
    assert.equal(getPointValue('N/S', false), 201,
            'Case N/S (feedback contribution not sure)');

    assert.equal(getPointValue('N/A', false), 202, 'Case N/A');

    assert.equal(getPointValue('0%', true), 0, 'Case 0% ditchZero true');
    assert.equal(getPointValue('0%', false), 100, 'Case 0% ditchZero false');
    assert.equal(getPointValue('1%', true), 101, 'Case 1%');
    assert.equal(getPointValue('-1%', true), 99, 'Case -1%');

    assert.equal(getPointValue('E -1%', false), 99, 'Case E -1%');
    assert.equal(getPointValue('E +1%', false), 101, 'Case E +1%');
    assert.equal(getPointValue('E +100%', false), 200, 'Case E +100%');
    assert.equal(getPointValue('E -100%', false), 0, 'Case E -100%');

    assert.equal(getPointValue('E', false), 100, 'Case E');

    assert.equal(getPointValue('0', false), 100, 'Integer 0');
    assert.equal(getPointValue('-1', false), 99, 'Integer -1');
    assert.equal(getPointValue('1', false), 101, 'Integer 1');

    function isCloseEnough(numberA, numberB) {
        var tolerance = 0.0001;
        return Math.abs(numberA - numberB) < tolerance;
    }
    assert.ok(isCloseEnough(getPointValue('0.0', false), 100), 'Float 0');
    assert.ok(isCloseEnough(getPointValue('0.1', false), 100.1), 'Float 0.1');
    assert.ok(isCloseEnough(getPointValue('-0.1', false), 99.9), 'Float -0.1');
    assert.ok(isCloseEnough(getPointValue('1.91', false), 101.91), 'Float 1.91');
    assert.ok(isCloseEnough(getPointValue('-1.22', false), 98.78), 'Float -1.22');
    assert.ok(isCloseEnough(getPointValue('3.833333', false), 103.833333), 'Float 3.833333');
    assert.ok(isCloseEnough(getPointValue('-3.833333', false), 96.166667), 'Float -3.833333');
});

QUnit.test('sortByPoint(a, b)', function(assert) {
    assert.ok(sortByPoint('N/S', 'N/A') < 0, 'Case N/S less than N/A');
    assert.ok(sortByPoint('N/S', 'E') > 0, 'N/S more than E');
    assert.ok(sortByPoint('N/A', 'E +1%') > 0, 'N/A more than E +(-)X%');
    assert.ok(sortByPoint('N/S', 'E -1%') > 0, 'N/S more than E +(-)X%');

    assert.ok(sortByPoint('0%', '0%') === 0, 'Case 0% equal 0%');
    assert.ok(sortByPoint('-1%', '0%') > 0, 'Case 0% less than every X%');
    assert.ok(sortByPoint('1%', '0%') > 0, 'Case 0% less than every X%');
    assert.ok(sortByPoint('3%', '-2%') > 0, 'Case 3% more than -2%');

    assert.ok(sortByPoint('E +1%', 'E') > 0, 'Case E +1% more than E');
    assert.ok(sortByPoint('E -1%', 'E') < 0, 'Case E -1% less than E');
    assert.ok(sortByPoint('E +33%', 'E -23%') > 0, 'Case E +33% more than E -23%');

    assert.ok(sortByPoint('0', '-1') > 0, 'Case Integer 0 more than -1');
    assert.ok(sortByPoint('1', '0') > 0, 'Case Integer 1 more than 0');
    assert.ok(sortByPoint('2', '-3') > 0, 'Case Integer 2 more than -3');

    assert.ok(sortByPoint('0.0', '1.0') < 0, 'Case Float 0.0 less than 1.0');
    assert.ok(sortByPoint('0.3', '-1.1') > 0, 'Case Float 0.3 more than -1.1');
    assert.ok(sortByPoint('0.3', '0.33338') < 0, 'Case Float 0.3 less than 0.33338');
    assert.ok(sortByPoint('-4.33333', '-4.5') > 0, 'Case Float -4.33333 more than -4.5');

    assert.ok(sortByPoint('NotNumber', 'Random') === 0, 'Equality for NaN');
});

QUnit.test('setStatusMessage(message,status)', function(assert) {
    $('body').append('<div id="statusMessagesToUser"></div>');
    var message = 'Status Message';

    // isError = false: class = overflow-auto alert alert-warning
    // isError = true: class = overflow-auto alert alert-danger

    setStatusMessage(message);
    assert.equal($('#statusMessagesToUser .statusMessage').html(), message, 'Normal status message');
    assert.ok($('#statusMessagesToUser .statusMessage').attr('class') === 'overflow-auto alert alert-info statusMessage',
        'Default message status without specifying status of message (info)');
    clearStatusMessages();

    setStatusMessage(message, StatusType.INFO);
    assert.equal($('#statusMessagesToUser .statusMessage').html(), message, 'Normal status message');
    assert.ok($('#statusMessagesToUser .statusMessage').attr('class') === 'overflow-auto alert alert-info statusMessage',
        'Info message status by specifying status of message (StatusType.INFO)');
    clearStatusMessages();

    setStatusMessage(message, StatusType.SUCCESS);
    assert.equal($('#statusMessagesToUser .statusMessage').html(), message, 'Normal status message');
    assert.ok($('#statusMessagesToUser .statusMessage').attr('class') === 'overflow-auto alert alert-success statusMessage',
        'Success message status by specifying status of message (StatusType.SUCCESS)');
    clearStatusMessages();

    setStatusMessage(message, StatusType.WARNING);
    assert.equal($('#statusMessagesToUser .statusMessage').html(), message, 'Normal status message');
    assert.ok($('#statusMessagesToUser .statusMessage').attr('class') === 'overflow-auto alert alert-warning statusMessage',
        'Warning message status by specifying status of message (StatusType.WARNING)');
    clearStatusMessages();

    setStatusMessage(message, StatusType.DANGER);
    assert.equal($('#statusMessagesToUser .statusMessage').html(), message, 'Normal status message');
    assert.ok($('#statusMessagesToUser .statusMessage').attr('class') === 'overflow-auto alert alert-danger statusMessage',
        'Danger message status by specifying status of message (StatusType.DANGER)');
    clearStatusMessages();

    setStatusMessage('');
    assert.equal($('#statusMessagesToUser .statusMessage').html(), undefined, 'Empty message');
    assert.ok($('#statusMessagesToUser .statusMessage').attr('class') === undefined, 'Empty message without status');
    clearStatusMessages();

    setStatusMessage('', StatusType.INFO);
    assert.equal($('#statusMessagesToUser .statusMessage').html(), undefined, 'Empty message');
    assert.ok($('#statusMessagesToUser .statusMessage').attr('class') === undefined,
              'Empty message with status (any status will be the same)');
    clearStatusMessages();

    setStatusMessage(message, 'random');
    assert.equal($('#statusMessagesToUser .statusMessage').html(), message, 'Normal status message');
    assert.ok($('#statusMessagesToUser .statusMessage').attr('class') === 'overflow-auto alert alert-info statusMessage',
              'Message with random status (defaulted to info)');
});

QUnit.test('clearStatusMessages()', function(assert) {
    clearStatusMessages();
    assert.equal($('#statusMessagesToUser').html(), '', 'Status message cleared');
    assert.ok($('#statusMessagesToUser').css('background-color') === 'rgba(0, 0, 0, 0)' || $(
        '#statusMessagesToUser').css('background-color') === 'transparent', 'No background');
});

QUnit.test('checkEvaluationForm()', function(assert) {
    // N/A, requires elements in the page
    assert.expect(0);
});

QUnit.test('addLoadingIndicator()', function(assert) {
    var $button = $('#loading-indicator');
    addLoadingIndicator($button, 'Loading');

    assert.equal($button.text(), 'Loading', 'Button text changes to loading');
    assert.equal($button.find('img').attr('src'), '/images/ajax-loader.gif', 'Loading gif appended');
    assert.ok($button.is(':disabled'), 'Button disabled');
});

QUnit.test('removeLoadingIndicator()', function(assert) {
    var $button = $('#loading-indicator');
    removeLoadingIndicator($button, 'Complete');

    assert.equal($button.text(), 'Complete', 'Button text changes to complete');
    assert.equal($button.find('img').length, 0, 'Loading gif removed');
    assert.notOk($button.is(':disabled'), 'Button enabled');
});

QUnit.test('sanitizeGoogleId(googleId)', function(assert) {
    assert.equal(sanitizeGoogleId('test  @Gmail.COM  '), 'test', 'test - valid');
    assert.equal(sanitizeGoogleId('  user@hotmail.com  '), 'user@hotmail.com',
        'user@hotmail.com - valid');
});

QUnit.test('isValidGoogleId(googleId)', function(assert) {
    assert.equal(isValidGoogleId('  test  \t\n'), true, 'test - valid');
    assert.equal(isValidGoogleId('  charile.brown  \t\n'), true, 'charile.brown - valid');
    assert.equal(isValidGoogleId('  big-small_mini  \t\n'), true, 'big-small_mini - valid');

    assert.equal(isValidGoogleId(' hello@GMail.COm \t\n '), false, 'hello@gmail.com - invalid');
    assert.equal(isValidGoogleId('wrong!'), false, 'wrong! - invalid');
    assert.equal(isValidGoogleId('not*correct'), false, 'not*correct - invalid');
    assert.equal(isValidGoogleId('is/not\\correct'), false, 'is/not\\correct - invalid');
});

QUnit.test('isEmailValid(email)', function(assert) {
    assert.equal(isEmailValid('test@gmail.com'), true, 'test@gmail.com - valid');
    assert.equal(isEmailValid('email'), false, 'email - invalid');
    assert.equal(isEmailValid('email@email'), false, 'email@email - invalid');
    assert.equal(isEmailValid('@yahoo.com'), false, '@yahoo.com - invalid');
    assert.equal(isEmailValid('email.com'), false, 'email.com - invalid');
});

QUnit.test('isNameValid(name)', function(assert) {
    assert.equal(isNameValid('\tTom Jacobs,.\t\'()-/ \\  '), true,
        'alphanumerics, fullstop, comma, round brackets, slashes, apostrophe, hyphen - valid');
    assert.equal(isNameValid(generateRandomString(NAME_MAX_LENGTH)), true,
        'Maximum characters - valid');

    assert.equal(isNameValid(''), false, 'Empty name - invalid');
    assert.equal(isNameValid(generateRandomString(NAME_MAX_LENGTH + 1)), false,
        'Exceed number of maximum characters - invalid');
    assert.equal(isNameValid('Tom! Jacobs'), false, '! character - invalid');
    assert.equal(isNameValid('Tom ^Jacobs'), false, '^ character - invalid');
    assert.equal(isNameValid('Tom#'), false, '# character - invalid');
    assert.equal(isNameValid('&Tom'), false, '& character - invalid');
    assert.equal(isNameValid('J%cobs '), false, '% character - invalid');
    assert.equal(isNameValid('Jacobs*'), false, '* character - invalid');
    assert.equal(isNameValid('\t+123\t '), false, '+ character - invalid');
    assert.equal(isNameValid('a b c $ 1 2 3 4'), false, '$ character - invalid');
    assert.equal(isNameValid('1@2@3  456'), false, '@ character - invalid');
    assert.equal(isNameValid('Tom = Tom'), false, '= character - invalid');
    assert.equal(isNameValid('Tom||Jacobs'), false, '| character - invalid');

});

QUnit.test('roundToThreeDp(num)', function(assert) {

    assert.equal(roundToThreeDp(0), 0, 'Zero test');
    assert.equal(roundToThreeDp(1), 1, 'Positive integer test');
    assert.equal(roundToThreeDp(-1), -1, 'Negative integer test');
    assert.equal(roundToThreeDp(1.001), 1.001, 'Three dp positive number test');
    assert.equal(roundToThreeDp(-1.001), -1.001, 'Three dp negative number test');
    assert.equal(roundToThreeDp(1.0015), 1.002, 'Four dp positive number rounding up test');
    assert.equal(roundToThreeDp(1.0011), 1.001, 'Four dp negative number rounding down test');
    assert.equal(roundToThreeDp(-1.0015), -1.002, 'Four dp positive number rounding "up" test');
    assert.equal(roundToThreeDp(-1.0011), -1.001, 'Four dp negative number rounding "down" test');

});

QUnit.test('sanitizeForJs(string)', function(assert) {
    assert.equal(sanitizeForJs(''), '', 'sanitization for empty string');
    assert.equal(sanitizeForJs('Will o\' Wisp'), 'Will o\\\' Wisp', 'sanitization for single quote');
    assert.equal(sanitizeForJs('Will o\'\'\'\'\'\\\\ Wisp'),
        'Will o\\\'\\\'\\\'\\\'\\\'\\\\\\\\ Wisp',
        'sanitization for single quote and slash \\');

});

QUnit.test('isBlank(string)', function(assert) {
    assert.equal(isBlank(''), true, 'Test - empty string');
    assert.equal(isBlank(' '), true, 'Test - single space');
    assert.equal(isBlank('            '), true, 'Test - multiple spaces');

    assert.equal(isBlank('test'), false, 'Test - not blank input');
    assert.equal(isBlank('test    test'), false, 'Test - spaces between strings');
    assert.equal(isBlank('     test'), false, 'Test - string with leading spaces');
    assert.equal(isBlank('test       '), false, 'Test - string with trailing spaces');
    assert.equal(isBlank('     test      '), false, 'Test - string with leading and trailing spaces');

    // type check
    assert.equal(isBlank(), false, 'Test - invalid type: empty input value');
    assert.equal(isBlank(null), false, 'Test - invalid type: null input');
    assert.equal(isBlank(0), false, 'Test - invalid type: number input');
    assert.equal(isBlank({}), false, 'Test - invalid type: object input');
    assert.equal(isBlank(undefined), false, 'Test - invalid type: undefined input');
});
