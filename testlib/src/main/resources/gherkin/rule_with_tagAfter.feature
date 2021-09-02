@tag_feature
Feature: Some tagged rules

  Rule: Untagged rule
  The untagged rule description

    Scenario: Scenario with only a feature tag
      Given a

  @tag_rule
  Rule: Tagged rule
  The tagged rule description

    Scenario: Scenario with feature and rule tags
      Given b

    @tag_scenario
    Scenario: Scenario with feature, rule and scenario tags
      Given b
