module.exports = function (api) {
	api.cache(true);
	return {
		presets: [["babel-preset-expo", { jsxRuntime: "automatic" }]],
		plugins: [/* MUST BE LAST */ "react-native-reanimated/plugin"],
	};
};
