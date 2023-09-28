type Name = "World" | "Maven" | "Gradle";
const foo = <T>(name: Name = "World", v: T): string => {
	return "Hello " + name;
};
