// Copyright 2017 The Bazel Authors. All rights reserved.
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//    http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.devtools.build.lib.rules.java;

import static com.google.devtools.build.lib.packages.Attribute.attr;
import static com.google.devtools.build.lib.packages.BuildType.LABEL;

import com.google.devtools.build.lib.analysis.BaseRuleClasses;
import com.google.devtools.build.lib.analysis.ConfiguredTarget;
import com.google.devtools.build.lib.analysis.FileProvider;
import com.google.devtools.build.lib.analysis.MiddlemanProvider;
import com.google.devtools.build.lib.analysis.RuleConfiguredTarget.Mode;
import com.google.devtools.build.lib.analysis.RuleConfiguredTargetBuilder;
import com.google.devtools.build.lib.analysis.RuleContext;
import com.google.devtools.build.lib.analysis.RuleDefinition;
import com.google.devtools.build.lib.analysis.RuleDefinitionEnvironment;
import com.google.devtools.build.lib.analysis.RunfilesProvider;
import com.google.devtools.build.lib.analysis.TransitiveInfoCollection;
import com.google.devtools.build.lib.collect.nestedset.NestedSetBuilder;
import com.google.devtools.build.lib.collect.nestedset.Order;
import com.google.devtools.build.lib.packages.RuleClass;
import com.google.devtools.build.lib.rules.MakeVariableProvider;
import com.google.devtools.build.lib.rules.RuleConfiguredTargetFactory;

/**
 * Implementation of the {@code java_runtime_alias} rule.
 */
public class JavaRuntimeAlias implements RuleConfiguredTargetFactory {

  @Override
  public ConfiguredTarget create(RuleContext ruleContext)
      throws InterruptedException, RuleErrorException {
    TransitiveInfoCollection runtime = ruleContext.getPrerequisite(":jvm", Mode.TARGET);
    // Sadly, we can't use an AliasConfiguredTarget here because we need to be prepared for the case
    // when --javabase is not a label. For the time being.
    RuleConfiguredTargetBuilder builder = new RuleConfiguredTargetBuilder(ruleContext);

    if (runtime != null) {
      MakeVariableProvider makeVariableProvider = runtime.getProvider(MakeVariableProvider.class);
      JavaRuntimeProvider javaRuntimeProvider = runtime.getProvider(JavaRuntimeProvider.class);
      builder
          .addProvider(javaRuntimeProvider)
          .addNativeDeclaredProvider(javaRuntimeProvider)
          .addProvider(makeVariableProvider)
          .addNativeDeclaredProvider(makeVariableProvider)
          .addProvider(RunfilesProvider.class, runtime.getProvider(RunfilesProvider.class))
          .addProvider(MiddlemanProvider.class, runtime.getProvider(MiddlemanProvider.class))
          .setFilesToBuild(runtime.getProvider(FileProvider.class).getFilesToBuild());
    } else {
      builder
          .setFilesToBuild(NestedSetBuilder.emptySet(Order.STABLE_ORDER))
          .addProvider(RunfilesProvider.class, RunfilesProvider.EMPTY);
    }

    return builder.build();
  }

  /**
   * Rule definition for the {@code java_runtime_alias} rule.
   */
  public static class JavaRuntimeAliasRule implements RuleDefinition {

    @Override
    public RuleClass build(RuleClass.Builder builder, RuleDefinitionEnvironment environment) {
      return builder
          .requiresConfigurationFragments(JavaConfiguration.class)
          .removeAttribute("licenses")
          .removeAttribute("distribs")
          .add(attr(":jvm", LABEL).value(JavaSemantics.jvmAttribute(environment)))
          .build();
    }

    @Override
    public Metadata getMetadata() {
      return Metadata.builder()
          .name("java_runtime_alias")
          .ancestors(BaseRuleClasses.BaseRule.class)
          .factoryClass(JavaRuntimeAlias.class)
          .build();
    }
  }
}
