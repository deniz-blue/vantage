import { ScrollView } from "react-native";
import { useQuery } from "@tanstack/react-query";
import { Box } from "../../base/Box";
import { Text } from "../../base/Text";
import { Divider } from "../../base/Divider";
import { Sizing } from "../../../theme/sizing";

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
			areaName: { value: string }[]
			country: { value: string }[]
			latitude: string
			longitude: string
			population: string
			region: { value: string }[]
			weatherUrl: { value: string }[]
		}[];
		request: { type: string; query: string }[];
		weather: Weather[];
	};

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
		hourly: Hourly[]
		maxtempC: string
		maxtempF: string
		mintempC: string
		mintempF: string
		sunHour: string
	};

	export interface Hourly {
		FeelsLikeC: string
		FeelsLikeF: string
		tempC: string
		tempF: string
		time: string
		weatherCode: string
		weatherDesc: { value: string }[]
		weatherIconUrl: { value: string }[]
		winddirDegree: string
		windspeedKmph: string
	}
}

const codeToEmoji = (codeStr: string) => {
	const code = Number(codeStr);
	switch (true) {
		case code == 113: return "☀️";
		case code == 116: return "⛅";
		case code == 119 || code == 122: return "☁️";
		case [143, 248, 260].includes(code): return "🌫️";
		case [176, 293, 296, 299, 302, 305, 308, 311, 314, 353].includes(code): return "🌦️";
		case [179, 182, 266, 281, 284].includes(code): return "🌨️";
		case [200, 386, 389].includes(code): return "⛈️";
		default: return "❓";
	}
};

const formatTime24 = (time: string) =>
	time.padStart(4, "0").replace(/(\d{2})(\d{2})/, "$1:$2");

export const WidgetWttrIn = () => {
	const query = useQuery({
		queryKey: ["wttr.in"],
		staleTime: 30 * 60 * 1000,
		queryFn: async () => {
			const response = await fetch("https://wttr.in/?format=j1");
			if (!response.ok) throw new Error("Failed to fetch weather data");
			return await response.json() as WttrIn.Response;
		},
	});

	const data = query.data;
	const current = data?.current_condition[0];
	const location = data
		? `${data.nearest_area[0].areaName[0].value}, ${data.nearest_area[0].country[0].value}`
		: "";
	const hourly = data?.weather[0].hourly ?? [];
	const feelsLike = current?.FeelsLikeC;
	const temp = current?.temp_C;
	const feelsNote = feelsLike !== temp ? ` (feels like ${feelsLike}°C)` : "";

	return (
		<Box gap="sm">
			<Divider px="md" label={<Text fz={Sizing.fontSizeSm} c="TextDimmed" fw="bold">Weather</Text>} />

			<Box bg="BackgroundLight" radius={Sizing.radiusSm} p="sm" gap="sm" mx="md">
				<Box direction="row" gap="sm" align="center">
					<Box
						w={48}
						h={48}
						radius={Sizing.radiusSm}
						bg="Background"
						align="center"
						justify="center"
					>
						<Text fz={28}>{current ? codeToEmoji(current.weatherCode) : "—"}</Text>
					</Box>
					<Box flex={1} gap={2}>
						<Text fz={Sizing.fontSizeSm} fw="bold">
							{location || "Weather"}
						</Text>
						<Text fz={Sizing.fontSizeSm} c="TextDimmed">
							{current
								? `${current.weatherDesc[0].value}, ${temp}°C${feelsNote}`
								: query.isLoading
									? "Loading…"
									: "No data"
							}
						</Text>
					</Box>
				</Box>
			</Box>

			{hourly.length > 0 && (
				<Box component={ScrollView} horizontal showsHorizontalScrollIndicator={false}>
					<Box direction="row" gap="sm" px="md">
						{hourly.map((hour) => (
							<Box
								key={hour.time}
								align="center"
								gap={4}
								bg="BackgroundLight"
								radius={Sizing.radiusSm}
								py="xs"
								w={48}
							>
								<Text fz={12} c="TextDimmed">
									{formatTime24(hour.time)}
								</Text>
								<Text fz={20}>{codeToEmoji(hour.weatherCode)}</Text>
								<Text fz={Sizing.fontSizeSm}>{hour.tempC}°</Text>
							</Box>
						))}
					</Box>
				</Box>
			)}
		</Box>
	);
};
