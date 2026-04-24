import { Stack, Text } from "@mantine/core";
import { SmallTitle } from "../../base/SmallTitle";
import { UtilEventSource, type EventSource } from "../../../../db/models/event-source";
import { IconAt, IconBraces, IconDatabase, IconExternalLink, IconWorld } from "@tabler/icons-react";
import { BaseSnippet } from "../../Snippet";
import { EventLinkButtonBase } from "../link/EventLinkButtonBase";
import { parseCanonicalResourceUri } from "@atcute/lexicons";
import { useQuery } from "@tanstack/react-query";
import { useAtProtoHandleQuery } from "../../../../lib/atproto/useAtProtoHandleQuery";
import { AtprotoDid } from "@atcute/lexicons/syntax";

export const EventDetailsSource = ({ source }: { source?: EventSource }) => {
	const atUri = source && UtilEventSource.getType(source) === "at" ? parseCanonicalResourceUri(source) : null;

	const handle = useAtProtoHandleQuery(atUri?.ok ? (atUri.value.repo as AtprotoDid) : undefined);

	return (
		<Stack gap={0} component="section">
			<SmallTitle>
				source
			</SmallTitle>

			{source && UtilEventSource.getType(source) === "local" && (
				<BaseSnippet icon={<IconDatabase />}>
					<Text inline>
						Browser/Device
					</Text>
				</BaseSnippet>
			)}

			{source && UtilEventSource.getType(source) === "at" && (
				<Stack gap={4}>
					<BaseSnippet icon={<IconAt />}>
						<Text inline>
							Atmosphere
						</Text>
					</BaseSnippet>

					{atUri && atUri.ok && (
						<EventLinkButtonBase
							url={`https://stargraph.link/user/${atUri.value.repo}`}
							leftSection={(
								<img
									src={`https://blobs.blue/${atUri.value.repo}/avatar-thumb`}
									alt={`Avatar of user ${atUri.value.repo}`}
									width={24}
									height={24}
									style={{ borderRadius: "50%" }}
								/>
							)}
						>
							<Text inherit span mr={4}>
								{handle.isSuccess ? handle.data : atUri.value.repo}
							</Text> <IconExternalLink size={14} />
						</EventLinkButtonBase>
					)}

					<EventLinkButtonBase
						url={`https://pds.ls/${source}`}
						leftSection={<img src="https://pds.ls/favicon.ico" alt="PDSls" width={24} height={24} />}
					>
						<Text inherit span mr={4}>
							View on PDSls
						</Text> <IconExternalLink size={14} />
					</EventLinkButtonBase>
				</Stack>
			)}

			{source && UtilEventSource.getType(source).startsWith("http") && (
				<Stack gap={4}>
					<BaseSnippet icon={<IconWorld />}>
						<Text inline>
							Internet
						</Text>
					</BaseSnippet>
					<EventLinkButtonBase
						leftSection={<IconBraces />}
						url={source}
					>
						<Text inherit span mr={4}>
							View raw data
						</Text> <IconExternalLink size={14} />
					</EventLinkButtonBase>
				</Stack>
			)}
		</Stack>
	)
};
