import { Button, Stack, Text } from "@mantine/core";
import { AsyncAction } from "../../../data/AsyncAction";

export const LayerImportSection = () => {
	// TODO

	return (
		<Stack>
			<AsyncAction action={async () => {
				
			}}>
				{({ loading, onClick }) => (
					<Button
						fullWidth
						onClick={onClick}
						loading={loading}
						color="green"
						h="auto"
					>
						<Stack gap={4} py={4}>
							<Text span inherit>
								Add to My Events
							</Text>
							<Text size="xs" fw="normal" span inherit>
								offline accessible and shown in your list
							</Text>
						</Stack>
					</Button>
				)}
			</AsyncAction>
		</Stack>
	);
};
