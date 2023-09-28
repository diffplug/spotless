export type Name = "World" | "Maven" | "Gradle";
export const foo = <T>(name: Name = "World", v: T): string => {
	return "Hello " + name;
};
