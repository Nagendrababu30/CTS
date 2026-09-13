(function () {

    "use strict";

    /*
     * ============================================================
     * MICR REPAIR - NUMERIC INPUT RESTRICTION
     * ============================================================
     *
     * Rules:
     *
     * 1. Only digits 0-9 are allowed.
     * 2. Maximum 3 digits.
     * 3. Characters must NOT appear in the textbox.
     * 4. Special characters must NOT appear.
     * 5. Space must NOT appear.
     * 6. Paste is also restricted.
     * 7. Delete / Backspace are allowed.
     * 8. Arrow/Home/End are allowed.
     * 9. Ctrl+A/C/V/X are allowed.
     *
     * Fields:
     *      ocrCityCode
     *      ocrBankCode
     *      ocrBranchCode
     * ============================================================
     */


    /**
     * Attach numeric restriction to one textbox.
     */
    function restrictMicrInput(input) {

        if (!input) {
            return;
        }

        /*
         * Prevent attaching the same handlers more than once.
         */
        if (input.getAttribute("data-micr-filter") === "true") {
            return;
        }

        input.setAttribute(
            "data-micr-filter",
            "true"
        );


        /*
         * ========================================================
         * KEYDOWN
         * ========================================================
         *
         * This is the important part.
         *
         * If user presses:
         *
         *      a
         *      h
         *      j
         *      @
         *      #
         *      -
         *      space
         *
         * preventDefault() is executed BEFORE the character
         * gets inserted into the textbox.
         */
        input.addEventListener(
            "keydown",
            function (event) {

                var key = event.key;


                /*
                 * Allow Backspace.
                 */
                if (key === "Backspace") {
                    return;
                }


                /*
                 * Allow Delete.
                 */
                if (key === "Delete") {
                    return;
                }


                /*
                 * Allow navigation keys.
                 */
                if (
                    key === "ArrowLeft" ||
                    key === "ArrowRight" ||
                    key === "ArrowUp" ||
                    key === "ArrowDown" ||
                    key === "Home" ||
                    key === "End"
                ) {
                    return;
                }


                /*
                 * Allow Tab.
                 */
                if (key === "Tab") {
                    return;
                }


                /*
                 * Allow Enter.
                 */
                if (key === "Enter") {
                    return;
                }


                /*
                 * Allow Escape.
                 */
                if (key === "Escape") {
                    return;
                }


                /*
                 * Allow Ctrl / Cmd shortcuts.
                 *
                 * Ctrl+A
                 * Ctrl+C
                 * Ctrl+V
                 * Ctrl+X
                 *
                 * Paste itself is handled separately below.
                 */
                if (
                    event.ctrlKey ||
                    event.metaKey
                ) {
                    return;
                }


                /*
                 * =================================================
                 * ONLY 0-9
                 * =================================================
                 *
                 * event.key for normal digits:
                 *
                 * "0"
                 * "1"
                 * ...
                 * "9"
                 *
                 * Anything else is blocked.
                 */
                if (!/^[0-9]$/.test(key)) {

                    event.preventDefault();

                    return false;
                }


                /*
                 * =================================================
                 * MAXIMUM 3 DIGITS
                 * =================================================
                 *
                 * If 3 digits already exist and the user has
                 * NOT selected anything, block the new digit.
                 */
                var start =
                    input.selectionStart;

                var end =
                    input.selectionEnd;

                var selectedLength =
                    end - start;


                if (
                    input.value.length >= 3 &&
                    selectedLength === 0
                ) {

                    event.preventDefault();

                    return false;
                }

            },
            true
        );


        /*
         * ========================================================
         * BEFORE INPUT
         * ========================================================
         *
         * This provides an additional browser-level protection.
         *
         * If the browser attempts to insert something other than
         * a digit, cancel the insertion.
         */
        input.addEventListener(
            "beforeinput",
            function (event) {

                /*
                 * Some browsers do not provide inputType/data.
                 */
                var data = event.data;


                /*
                 * Deletion operations are allowed.
                 */
                if (
                    event.inputType === "deleteContentBackward" ||
                    event.inputType === "deleteContentForward" ||
                    event.inputType === "deleteByCut"
                ) {
                    return;
                }


                /*
                 * If actual inserted data exists,
                 * it must contain only digits.
                 */
                if (
                    data !== null &&
                    data !== undefined &&
                    data !== ""
                ) {

                    if (!/^[0-9]+$/.test(data)) {

                        event.preventDefault();

                        return false;
                    }


                    /*
                     * Calculate resulting length.
                     */
                    var start =
                        input.selectionStart || 0;

                    var end =
                        input.selectionEnd || 0;

                    var selectedLength =
                        end - start;

                    var resultingLength =
                        input.value.length -
                        selectedLength +
                        data.length;


                    if (resultingLength > 3) {

                        event.preventDefault();

                        return false;
                    }
                }

            },
            true
        );


        /*
         * ========================================================
         * PASTE
         * ========================================================
         *
         * Example:
         *
         *     ABC123
         *          ↓
         *         123
         *
         *     12ABC
         *          ↓
         *         12
         *
         *     123456
         *          ↓
         *         123
         */
        input.addEventListener(
            "paste",
            function (event) {

                event.preventDefault();


                var clipboardData =
                    event.clipboardData ||
                    window.clipboardData;


                if (!clipboardData) {
                    return;
                }


                var pasted =
                    clipboardData.getData("text");


                if (!pasted) {
                    return;
                }


                /*
                 * Remove everything except digits.
                 */
                var digits =
                    pasted
                        .replace(/[^0-9]/g, "")
                        .substring(0, 3);


                /*
                 * Current selection.
                 */
                var start =
                    input.selectionStart || 0;

                var end =
                    input.selectionEnd || 0;


                var currentValue =
                    input.value || "";


                /*
                 * Replace selected text with pasted digits.
                 */
                var newValue =
                    currentValue.substring(
                        0,
                        start
                    )
                    +
                    digits
                    +
                    currentValue.substring(
                        end
                    );


                /*
                 * Final safety:
                 *
                 * digits only
                 * maximum 3
                 */
                newValue =
                    newValue
                        .replace(/[^0-9]/g, "")
                        .substring(0, 3);


                input.value =
                    newValue;


                /*
                 * Notify ZK/browser that the value changed.
                 */
                try {

                    input.dispatchEvent(
                        new Event(
                            "input",
                            {
                                bubbles: true
                            }
                        )
                    );

                } catch (e) {

                    /*
                     * Compatibility fallback.
                     */
                    var inputEvent =
                        document.createEvent(
                            "Event"
                        );

                    inputEvent.initEvent(
                        "input",
                        true,
                        true
                    );

                    input.dispatchEvent(
                        inputEvent
                    );
                }

            },
            true
        );


        /*
         * ========================================================
         * INPUT
         * ========================================================
         *
         * Final safety net.
         *
         * This catches:
         *
         * - drag/drop
         * - browser autofill
         * - mobile keyboard input
         * - other programmatic insertion
         */
        input.addEventListener(
            "input",
            function () {

                var oldValue =
                    input.value || "";


                var newValue =
                    oldValue
                        .replace(/[^0-9]/g, "")
                        .substring(0, 3);


                if (oldValue !== newValue) {

                    input.value =
                        newValue;
                }

            },
            true
        );

    }


    /**
     * ============================================================
     * Find the actual HTML input generated by ZK.
     * ============================================================
     */
    function attachToZkTextbox(widgetId) {

        var widget =
            zk.Widget.$(
                "$" + widgetId
            );


        if (!widget) {
            return;
        }


        var input =
            widget.$n();


        if (!input) {
            return;
        }


        restrictMicrInput(input);
    }


    /**
     * ============================================================
     * Attach all MICR fields.
     * ============================================================
     */
    function attachMicrFields() {

        if (
            typeof zk === "undefined" ||
            !zk.Widget
        ) {
            return;
        }


        attachToZkTextbox(
            "ocrCityCode"
        );


        attachToZkTextbox(
            "ocrBankCode"
        );


        attachToZkTextbox(
            "ocrBranchCode"
        );

    }


    /**
     * Disable Grammarly on the remarks textbox
     */
    function disableGrammarlyOnRemarks() {
        var elements = document.querySelectorAll(
            '.return-textbox, [id$="returnRemarks"], textarea[name*="returnRemarks"]'
        );
        elements.forEach(function (el) {
            el.setAttribute("data-gramm", "false");
            el.setAttribute("data-gramm_editor", "false");
            el.setAttribute("data-enable-grammarly", "false");
            el.setAttribute("spellcheck", "false");
            el.setAttribute("autocomplete", "off");
        });
    }

    /**
     * ============================================================
     * ZK AFTER MOUNT
     * ============================================================
     *
     * Wait until ZK has created the actual HTML elements.
     */
    function initializeMicrInputFilter() {

        attachMicrFields();
        disableGrammarlyOnRemarks();

        // Ensure Grammarly stays disabled whenever the return dialog opens or is focused
        document.addEventListener("focusin", function (e) {
            if (e.target && (e.target.classList.contains("return-textbox") || (e.target.id && e.target.id.indexOf("returnRemarks") !== -1))) {
                disableGrammarlyOnRemarks();
            }
        }, true);

        document.addEventListener("click", function (e) {
            if (e.target && e.target.id && e.target.id.indexOf("returnButton") !== -1) {
                setTimeout(disableGrammarlyOnRemarks, 150);
            }
        });
    }


    /*
     * ZK initialization.
     */
    if (
        typeof zk !== "undefined" &&
        zk.afterMount
    ) {

        zk.afterMount(
            initializeMicrInputFilter
        );

    } else {

        /*
         * Fallback if this JS loads before ZK.
         */
        window.addEventListener(
            "load",
            initializeMicrInputFilter
        );
    }


    /*
     * Make the function available globally only if needed.
     */
    window.MicrRepairInputFilter = {
        initialize: initializeMicrInputFilter
    };


})();