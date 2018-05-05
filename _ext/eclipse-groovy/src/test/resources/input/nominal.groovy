class TestClass {
	def a;
	
	TestClass(String s) {
		this.a = s
	}
	def methodNamedArgs(Map args) {
		"named args: $args"
	}
}

def t = new TestClass()
def arr = [4, 'string1', 'string2']
