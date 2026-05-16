import { Center, Divider, Group, Image, Loader, Paper, ScrollArea, Stack, Text, Title } from "@mantine/core";
import { useQuery } from "@tanstack/react-query";
import { SmallTitle } from "../../../content/base/SmallTitle";

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
};

export const WidgetWttrIn = () => {
	const codeToEmoji = (codeStr: string) => {
		const code = Number(codeStr);
		switch (true) {
			case code == 113: return "☀️"; // Clear/Sunny
			case code == 116: return "⛅"; // Partly Cloudy
			case code == 119 || code == 122: return "☁️"; // Cloudy/Overcast
			case [143, 248, 260].includes(code): return "🌫️"; // Mist/Fog
			case [176, 293, 296, 299, 302, 305, 308, 311, 314, 353].includes(code): return "🌦️"; // Rain-ish
			case [179, 182, 266, 281, 284].includes(code): return "🌨️"; // Snow-ish
			case [200, 386, 389].includes(code): return "⛈️"; // Thunderstorm
			default: return "❓";
		}
	};

	const query = useQuery({
		queryKey: ["wttr.in"],
		staleTime: 30 * 60 * 1000, // 30 minutes
		queryFn: async () => {
			const response = await fetch("https://wttr.in/?format=j1");
			if (!response.ok) throw new Error("Failed to fetch weather data");
			return await response.json() as WttrIn.Response;
		},
	});

	const weatherCode = query.data?.current_condition[0].weatherCode;
	const weatherDescription = query.data?.current_condition[0].weatherDesc[0].value;
	const temperatureC = query.data?.current_condition[0].temp_C;
	const feelsLikeC = query.data?.current_condition[0].FeelsLikeC;
	const location = query.data ? `${query.data.nearest_area[0].areaName[0].value}, ${query.data.nearest_area[0].country[0].value}` : "";
	const windSpeedKmph = query.data?.current_condition[0].windspeedKmph;
	const windDirectionDegree = query.data?.current_condition[0].winddirDegree;
	const hourly = query.data?.weather[0].hourly ?? [];

	return (
		<Stack gap="xs">
			<Divider
				labelPosition="left"
				variant="dashed"
				label={(
					<Title order={4}>
						Weather
					</Title>
				)}
			/>

			<Paper p="xs" bg="dark.8">
				<Group align="start" gap="xs">
					<Paper
						w="4em"
						h="4em"
						radius="md"
						withBorder
						bg="dark.6"
					>
						<Center h="100%">
							{query.isLoading ? (
								<Loader />
							) : (
								<Text span inline inherit fz="2em" ta="center" style={{ pointerEvents: "none", userSelect: "none", touchAction: "none" }}>
									{codeToEmoji(weatherCode || "0")}
								</Text>
							)}
						</Center>
					</Paper>
					<Stack gap={0} fz="sm">
						<SmallTitle py={0}>
							{location}
						</SmallTitle>

						<Text span inherit>
							{weatherDescription}, {temperatureC}°C {temperatureC !== feelsLikeC && `(feels like ${feelsLikeC}°C)`}
						</Text>
						<Text span inherit>
							Wind: {windSpeedKmph} km/h <span
								aria-label={`Direction: ${windDirectionDegree} degrees`}
								style={{
									transform: `rotate(${windDirectionDegree}deg)`,
									display: "inline-block",
									marginLeft: 4,
								}}
							>↑</span>
						</Text>
					</Stack>
				</Group>
			</Paper>

			<Paper bg="dark.8">
				<ScrollArea.Autosize maw="100%" scrollbars="x" offsetScrollbars p={4}>
					<Group wrap="nowrap" align="stretch" justify="center" px="xs" pt="xs">
						{hourly.map((hour) => (
							<Stack gap={0} key={hour.time}>
								<Text span fz="sm" ta="center" ff="monospace">
									{hour.time.padStart(4, "0").replace(/(\d{2})(\d{2})/, "$1:$2")}
								</Text>
								<Paper key={hour.time} withBorder p="xs" miw="3rem" mih="3rem">
									<Center h="100%">
										<Stack gap={4} align="center">
											<Text span fz="1.5em" inline>
												{codeToEmoji(hour.weatherCode)}
											</Text>
											<Text span inline fz="sm">
												{hour.tempC}°C
											</Text>
										</Stack>
									</Center>
								</Paper>
							</Stack>
						))}
					</Group>
				</ScrollArea.Autosize>
			</Paper>
		</Stack>
	);
};
