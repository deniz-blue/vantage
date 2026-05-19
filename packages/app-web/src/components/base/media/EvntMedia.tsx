import type { Media } from "@evnt/schema";
import { Box, Skeleton } from "@mantine/core";
import { Blurhash } from "./Blurhash";
import { OverLayer } from "../layout/OverLayer";
import { ReactNode, useState } from "react";

export const EvntMedia = ({
	media,
	objectFit = "cover",
	coverElement,
}: {
	media: Media;
	objectFit?: React.CSSProperties["objectFit"];
	coverElement?: ReactNode;
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
					<Skeleton w="100%" h="100%" />
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

			{!!coverElement && state === "loaded" && (
				coverElement
			)}
		</Box>
	);
};
