module.exports = {
	env: {
		browser: true,
		es2021: true,
	},
	extends: 'xo/browser',
	overrides: [
		{
			extends: [
				'xo-typescript',
			],
			files: [
				'*.ts',
				'*.tsx',
			],
		},
	],
	parser: "@typescript-eslint/parser",
	parserOptions: {
		ecmaVersion: 'latest',
		sourceType: 'module',
		project: './tsconfig.json',
	},
	rules: {
	},
};
