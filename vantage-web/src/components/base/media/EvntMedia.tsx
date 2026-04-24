import type { Media } from "@evnt/schema";
import { Box, Center, Image } from "@mantine/core";
import { Blurhash } from "./Blurhash";
import { OverLayer } from "../layout/OverLayer";
import { useState } from "react";
import { CenteredLoader } from "../../content/base/CenteredLoader";
import { IconPhotoOff } from "@tabler/icons-react";

export const EvntMedia = ({
	media,
	objectFit = "cover",
}: {
	media: Media;
	objectFit?: React.CSSProperties["objectFit"];
}) => {
	const [state, setState] = useState<"loading" | "loaded" | "error">("loading");

	const srcset = media.sources.map(source => (
		source.url + (source.dimensions?.width ? ` ${source.dimensions.width}w` : "")
	)).join(", ");

	return (
		<Box w="100%" h="100%" pos="relative">
			{!!media.presentation?.blurhash && (
				<OverLayer>
					<Blurhash hash={media.presentation.blurhash} />
				</OverLayer>
			)}

			{state === "loading" && (
				<OverLayer opacity={0.4}>
					<CenteredLoader loaderColor="black" />
				</OverLayer>
			)}

			{state === "error" && (
				<OverLayer opacity={0.4}>
					<Center h="100%">
						<IconPhotoOff size={48} />
					</Center>
				</OverLayer>
			)}

			<OverLayer>
				{state !== "error" && (
					<Box
						component="img"
						src={media.sources[0]?.url}
						srcSet={srcset}
						style={{ objectFit }}
						loading="lazy"
						w="100%"
						h="100%"
						onLoad={() => setState("loaded")}
						onError={() => setState("error")}
					/>
				)}
			</OverLayer>
		</Box>
	);
};
