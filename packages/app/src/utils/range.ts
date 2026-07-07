export const range = (until: number) =>
	Array.from({ length: until })
		.fill(0)
		.map((_, i) => i);
