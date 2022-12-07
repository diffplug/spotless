module.exports = {
	"env": {
		"browser": true,
		"es2021": true
	},
	"extends": [
		"eslint:recommended",
		"plugin:@typescript-eslint/recommended"
	],
	"overrides": [
	],
	"parser": "@typescript-eslint/parser",
	"parserOptions": {
		"ecmaVersion": "latest",
		"sourceType": "module"
	},
	"plugins": [
		"@typescript-eslint"
	],
	"rules": {
		"indent": [
			"error",
			4
		],
		"linebreak-style": [
			"error",
			"unix"
		],
		"quotes": [
			"error",
			"double"
		],
		"semi": [
			"error",
			"always"
		],
		"curly": [
			"error"
		],
		"max-statements-per-line": [
			"error",
			{ "max": 1 }
		],
		"object-curly-newline": [
			"error",
			"always"
		],
		"comma-spacing": [
			"error",
			{ "before": false, "after": true }
		],
		"object-property-newline": [
			"error",
		],
		"no-trailing-spaces": [
			"error"
		],
	}
};
