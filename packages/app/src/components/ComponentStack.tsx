import { ComponentType, PropsWithChildren, ComponentProps } from "react";

export const ComponentStack = <TStack extends readonly ComponentType<any>[]>({
	children,
	stack,
}: PropsWithChildren<{
	stack: {
		[I in keyof TStack]: [TStack[I], Omit<ComponentProps<TStack[I]>, "children">];
	};
}>) => {
	return stack.reduceRight(
		(prev, [Component, props]) => <Component {...props}>{prev}</Component>,
		children,
	);
};
