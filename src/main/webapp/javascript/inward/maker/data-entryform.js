function ctsConvertToIndianWords(amountStr) {
			if (!amountStr) return "";
			var clean = (amountStr + "").replace(/,/g, "").trim();
			var num = parseFloat(clean);
			if (isNaN(num) || num <= 0) return "";

			var rupees = Math.floor(num);
			var paise = Math.round((num - rupees) * 100);

			var units = ["", "One", "Two", "Three", "Four", "Five", "Six", "Seven", "Eight", "Nine",
				"Ten", "Eleven", "Twelve", "Thirteen", "Fourteen", "Fifteen", "Sixteen",
				"Seventeen", "Eighteen", "Nineteen"];
			var tens = ["", "", "Twenty", "Thirty", "Forty", "Fifty", "Sixty", "Seventy", "Eighty", "Ninety"];

			function convertUnderThousand(n) {
				var str = "";
				if (n >= 100) {
					str += units[Math.floor(n / 100)] + " Hundred ";
					n %= 100;
					if (n > 0) str += "and ";
				}
				if (n >= 20) {
					str += tens[Math.floor(n / 10)];
					if (n % 10 > 0) str += " " + units[n % 10];
				} else if (n > 0) {
					str += units[n];
				}
				return str.trim();
			}

			function convertToIndianFormat(n) {
				if (n === 0) return "";
				var str = "";
				if (n >= 10000000) {
					str += convertToIndianFormat(Math.floor(n / 10000000)) + " Crore ";
					n %= 10000000;
				}
				if (n >= 100000) {
					str += convertToIndianFormat(Math.floor(n / 100000)) + " Lakh ";
					n %= 100000;
				}
				if (n >= 1000) {
					str += convertToIndianFormat(Math.floor(n / 1000)) + " Thousand ";
					n %= 1000;
				}
				if (n > 0) {
					str += convertUnderThousand(n);
				}
				return str.trim();
			}

			var words = (rupees === 0 ? "Zero" : convertToIndianFormat(rupees)) + " Rupees";
			if (paise > 0) {
				words += " and " + convertToIndianFormat(paise) + " Paise";
			}
			words += " Only";
			return words.toUpperCase();
		}

		function updateAmountWordsFromInput(inputNode) {
			var raw = inputNode ? inputNode.value : "";
			var words = "";
			if (raw && raw.trim() !== "") {
				words = ctsConvertToIndianWords(raw);
			}
			var wgt = zk.Widget.$('$txtAmountInWords');
			if (wgt) {
				wgt.setValue(words);
				var domNode = wgt.getInputNode ? wgt.getInputNode() : (wgt.$n ? wgt.$n() : null);
				if (domNode) {
					domNode.value = words;
				}
			}
		}

		zk.afterMount(function() {
			var decWgt = zk.Widget.$('$decAmount');
			if (decWgt) {
				var inp = decWgt.getInputNode ? decWgt.getInputNode() : decWgt.$n();
				if (inp) {
					var handler = function() {
						updateAmountWordsFromInput(inp);
					};
					inp.addEventListener('input', handler);
					inp.addEventListener('keyup', handler);
					inp.addEventListener('change', handler);
					setTimeout(function() {
						updateAmountWordsFromInput(inp);
					}, 300);
				}
			}
		});