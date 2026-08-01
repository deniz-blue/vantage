import { useQuery } from "@tanstack/react-query";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Line } from "../../base/Divider";
import { FontSize, Radius } from "../../../theme/sizing";
import { Loader } from "../../base/Loader";

export namespace WttrIn {
	export interface Response {
		current_condition: {
			FeelsLikeC: string;
			FeelsLikeF: string;
			temp_C: string;
			temp_F: string;
			weatherDesc: { value: string }[];
			weatherIconUrl: { value: string }[];
			winddirDegree: string;
			windspeedKmph: string;
			weatherCode: string;
		}[];
		nearest_area: {
			areaName: { value: string }[];
			country: { value: string }[];
			latitude: string;
			longitude: string;
			population: string;
			region: { value: string }[];
			weatherUrl: { value: string }[];
		}[];
		request: { type: string; query: string }[];
		weather: Weather[];
	}

	export interface Weather {
		astronomy: {
			moon_illumination: string;
			moon_phase: string;
			moonrise: string;
			moonset: string;
			sunrise: string;
			sunset: string;
		}[];
		avgtempC: string;
		avgtempF: string;
		date: string;
		hourly: Hourly[];
		maxtempC: string;
		maxtempF: string;
		mintempC: string;
		mintempF: string;
		sunHour: string;
	}

	export interface Hourly {
		FeelsLikeC: string;
		FeelsLikeF: string;
		tempC: string;
		tempF: string;
		time: string;
		weatherCode: string;
		weatherDesc: { value: string }[];
		weatherIconUrl: { value: string }[];
		winddirDegree: string;
		windspeedKmph: string;
	}
}

const codeToEmoji = (codeStr: string) => {
	const code = Number(codeStr);
	switch (true) {
		case code == 113:
			return "☀️";
		case code == 116:
			return "⛅";
		case code == 119 || code == 122:
			return "☁️";
		case [143, 248, 260].includes(code):
			return "🌫️";
		case [176, 293, 296, 299, 302, 305, 308, 311, 314, 353].includes(code):
			return "🌦️";
		case [179, 182, 266, 281, 284].includes(code):
			return "🌨️";
		case [200, 386, 389].includes(code):
			return "⛈️";
		default:
			return "❓";
	}
};

export const WidgetWttrIn = () => {
	const query = useQuery({
		queryKey: ["wttr.in"],
		staleTime: 30 * 60 * 1000,
		queryFn: async () => {
			const response = await fetch("https://wttr.in/?format=j1");
			if (!response.ok) throw new Error("Failed to fetch weather data");
			return (await response.json()) as WttrIn.Response;
		},
	});

	const data = query.data;
	const current = data?.current_condition[0];
	const location = data
		? `${data.nearest_area[0].areaName[0].value}, ${data.nearest_area[0].country[0].value}`
		: "";
	// const hourly = data?.weather[0].hourly ?? [];
	const feelsLike = current?.FeelsLikeC;
	const temp = current?.temp_C;
	const feelsNote = feelsLike !== temp ? ` (feels like ${feelsLike}°C)` : "";

	return (
		<Box gap={0}>
			<Box direction="row" align="center" px="md" py="sm" gap="sm">
				<Text fw="bold">Weather</Text>
				<Line />
			</Box>

			<Box bg="BackgroundLight" radius={Radius.sm} p="sm" gap="sm" mx="md">
				<Box direction="row" gap="sm" align="center">
					<Box w={48} h={48} radius={Radius.sm} bg="Background" align="center" justify="center">
						{current ? (
							<Text fz={28} aria-hidden>
								{codeToEmoji(current.weatherCode)}
							</Text>
						) : query.isLoading ? (
							<Loader />
						) : (
							<Text fz={28} aria-hidden>
								❓
							</Text>
						)}
					</Box>
					<Box flex={1} gap={2}>
						<Text fz={FontSize.sm} fw="bold">
							{location || "Weather"}
						</Text>
						<Text fz={FontSize.sm} c="TextDimmed">
							{current
								? `${current.weatherDesc[0].value}, ${temp}°C${feelsNote}`
								: query.isLoading
									? "Loading…"
									: "No data"}
						</Text>
					</Box>
				</Box>
			</Box>
		</Box>
	);
};
