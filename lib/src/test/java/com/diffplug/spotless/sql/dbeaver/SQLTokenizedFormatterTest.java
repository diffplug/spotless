package com.diffplug.spotless.sql.dbeaver;

import static org.assertj.core.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Properties;

class SQLTokenizedFormatterTest {
	private SQLTokenizedFormatter formatter;


	@BeforeEach
	void setUp() {
		Properties properties = new Properties();
		properties.setProperty("sql.formatter.keyword.case", "UPPER");
		properties.setProperty("sql.formatter.statement.delimiter", ";");
		properties.setProperty("sql.formatter.indent.type", "space");
		properties.setProperty("sql.formatter.indent.size", "4");

		DBeaverSQLFormatterConfiguration config = new DBeaverSQLFormatterConfiguration(properties);
		formatter = new SQLTokenizedFormatter(config);
	}

	@Test
	void testSimpleSelect() {
		String input = "SELECT * FROM table";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT *\nFROM table");
	}

	@Test
	void testSelectWithWhere() {
		String input = "SELECT id, name FROM users WHERE age > 18";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT id, name\nFROM users\nWHERE age > 18");
	}

	@Test
	void testNestedSelect() {
		String input = "SELECT a FROM (SELECT b FROM table2) t";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT a\nFROM (\n    SELECT b\n    FROM table2\n) t");
	}

	@Test
	void testDeeplyNestedQuery() {
		String input = "SELECT * FROM (SELECT * FROM (SELECT * FROM table))";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT *\nFROM (\n    SELECT *\n    FROM (\n        SELECT *\n        FROM table\n    )\n)");
	}

	@Test
	void testInnerJoin() {
		String input = "SELECT u.name, o.amount FROM users u JOIN orders o ON u.id = o.user_id";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT u.name, o.amount\nFROM users u\nJOIN orders o\n    ON u.id = o.user_id");
	}

	@Test
	void testLeftOuterJoin() {
		String input = "SELECT u.name, o.amount FROM users u LEFT OUTER JOIN orders o ON u.id = o.user_id";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT u.name, o.amount\nFROM users u\nLEFT OUTER JOIN orders o\n    ON u.id = o.user_id");
	}

	@Test
	void testFunctionCall() {
		String input = "SELECT COUNT(*), MAX(age) FROM users";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT COUNT(*), MAX(age)\nFROM users");
	}

	@Test
	void testCommaSeparatedList() {
		String input = "SELECT id, name, age, email FROM users";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT id, name, age, email\nFROM users");
	}

	@Test
	void testSingleLineComment() {
		String input = "SELECT * FROM users -- This is a comment";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT *\nFROM users -- This is a comment");
	}

	@Test
	void testMultiLineComment() {
		String input = "SELECT * FROM /* Multi-line \n comment */ users";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT *\nFROM /* Multi-line \n comment */ users");
	}

	@Test
	void testEmptyInput() {
		assertThat(formatter.format(""))
			.isEmpty();
	}

	@Test
	void testUnclosedParenthesis() {
		String input = "SELECT * FROM (SELECT id FROM users";
		assertThatCode(() -> formatter.format(input))
			.doesNotThrowAnyException();
	}

	@Test
	void testKeywordUppercase() {
		String input = "select * from users";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT *\nFROM users");
	}

	@Test
	void testMixedCaseKeywords() {
		String input = "SeLeCt * FrOm users";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT *\nFROM users");
	}

	@Test
	void testComplexQuery() {
		String input = "SELECT u.name, COUNT(o.id) FROM users u LEFT JOIN orders o ON u.id = o.user_id WHERE u.age > 18 GROUP BY u.name HAVING COUNT(o.id) > 5 ORDER BY u.name";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT u.name, COUNT(o.id)\n" +
				"FROM users u\n" +
				"LEFT JOIN orders o\n" +
				"    ON u.id = o.user_id\n" +
				"WHERE u.age > 18\n" +
				"GROUP BY u.name\n" +
				"HAVING COUNT(o.id) > 5\n" +
				"ORDER BY u.name");
	}

	@Test
	void testStatementDelimiter() {
		String input = "SELECT * FROM users; SELECT * FROM orders;";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT *\nFROM users;\n\nSELECT *\nFROM orders;");
	}

	@Test
	void testBetweenClause() {
		String input = "SELECT * FROM products WHERE price BETWEEN 10 AND 100";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT *\nFROM products\nWHERE price BETWEEN 10 AND 100");
	}

	@Test
	void testGroupByOrderBy() {
		String input = "SELECT department, COUNT(*) FROM employees GROUP BY department ORDER BY COUNT(*) DESC";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT department, COUNT(*)\n" +
				"FROM employees\n" +
				"GROUP BY department\n" +
				"ORDER BY COUNT(*) DESC");
	}

	@Test
	void testSubqueryInWhere() {
		String input = "SELECT name FROM products WHERE category_id IN (SELECT id FROM categories WHERE active = true)";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT name\n" +
				"FROM products\n" +
				"WHERE category_id IN (\n" +
				"    SELECT id\n" +
				"    FROM categories\n" +
				"    WHERE active = true\n" +
				")");
	}

	@Test
	void testCaseStatement() {
		String input = "SELECT name, CASE WHEN age < 18 THEN 'minor' WHEN age < 65 THEN 'adult' ELSE 'senior' END AS age_group FROM users";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT name, CASE\n" +
				"    WHEN age < 18 THEN 'minor'\n" +
				"    WHEN age < 65 THEN 'adult'\n" +
				"    ELSE 'senior'\n" +
				"END AS age_group\n" +
				"FROM users");
	}

	@Test
	void testUnionQuery() {
		String input = "SELECT name FROM active_users UNION SELECT name FROM inactive_users";
		assertThat(formatter.format(input))
			.isEqualTo("SELECT name\n" +
				"FROM active_users\n" +
				"UNION\n" +
				"SELECT name\n" +
				"FROM inactive_users");
	}

	@Test
	void testCreateTable() {
		String input = "CREATE TABLE users (id INT PRIMARY KEY, name VARCHAR(100), email VARCHAR(255))";
		assertThat(formatter.format(input))
			.isEqualTo("CREATE TABLE users (\n" +
				"    id INT PRIMARY KEY,\n" +
				"    name VARCHAR(100),\n" +
				"    email VARCHAR(255)\n" +
				")");
	}

	@Test
	void testInsertStatement() {
		String input = "INSERT INTO users (id, name, email) VALUES (1, 'John', 'john@example.com')";
		assertThat(formatter.format(input))
			.isEqualTo("INSERT INTO users (\n" +
				"    id, name, email\n" +
				") VALUES (\n" +
				"    1, 'John', 'john@example.com'\n" +
				")");
	}

	@Test
	void testWithClause() {
		String input = "WITH temp AS (SELECT * FROM users) SELECT * FROM temp";
		assertThat(formatter.format(input))
			.isEqualTo("WITH temp AS (\n" +
				"    SELECT * FROM users\n" +
				") SELECT * FROM temp");
	}

	@Test
	void testMultipleWithClauses() {
		String input = "WITH users_18 AS (SELECT * FROM users WHERE age = 18), users_19 AS (SELECT * FROM users WHERE age = 19) SELECT * FROM users_18 UNION SELECT * FROM users_19";
		assertThat(formatter.format(input))
			.isEqualTo("WITH users_18 AS (\n" +
				"    SELECT * FROM users WHERE age = 18\n" +
				"), users_19 AS (\n" +
				"    SELECT * FROM users WHERE age = 19\n" +
				") SELECT * FROM users_18\n" +
				"UNION\n" +
				"SELECT * FROM users_19");
	}
}
