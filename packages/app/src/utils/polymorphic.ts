import type { ComponentPropsWithoutRef, ElementType } from "react";

/**
 * Adds a polymorphic `component` prop to a component's props,
 * inheriting the props of `DefaultElement` as the base.
 *
 * `component` is deliberately unconstrained (`ElementType`) so consumers
 * can plug in any native element. Their types won't be validated against
 * the component's internals, but the passed element must render the props
 * you're using (common RN surface area like `style`, `onPress`, etc. mostly overlap).
 *
 * @example
 * ```ts
 * interface ButtonProps extends PolymorphicProps<typeof TouchableOpacity, {
 *   variant?: "primary" | "secondary";
 * }> {}
 *
 * const Button = ({ component: Component = TouchableOpacity, variant, ...rest }: ButtonProps) => (
 *   <Box component={Component} {...rest as any} />
 * );
 * ```
 */
export type PolymorphicProps<
	DefaultElement extends ElementType,
	CustomProps = {},
> = CustomProps &
	Omit<ComponentPropsWithoutRef<DefaultElement>, keyof CustomProps | "component"> & {
		component?: ElementType;
	};
