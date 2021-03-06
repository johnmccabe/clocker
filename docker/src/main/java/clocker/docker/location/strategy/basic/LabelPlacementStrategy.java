/*
 * Copyright 2014-2016 by Cloudsoft Corporation Limited
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package clocker.docker.location.strategy.basic;

import java.util.List;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import clocker.docker.location.DockerHostLocation;
import clocker.docker.location.strategy.BasicDockerPlacementStrategy;

import com.google.common.base.Function;
import com.google.common.base.Splitter;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.reflect.TypeToken;

import org.jclouds.compute.domain.NodeMetadata;

import org.apache.brooklyn.config.ConfigKey;
import org.apache.brooklyn.core.config.ConfigKeys;
import org.apache.brooklyn.location.jclouds.JcloudsSshMachineLocation;
import org.apache.brooklyn.location.ssh.SshMachineLocation;
import org.apache.brooklyn.util.collections.MutableSet;
import org.apache.brooklyn.util.core.flags.SetFromFlag;

/**
 * Placement strategy that checks labels on hosts.
 */
public class LabelPlacementStrategy extends BasicDockerPlacementStrategy {

    private static final Logger LOG = LoggerFactory.getLogger(LabelPlacementStrategy.class);

    @SetFromFlag("labels")
    public static final ConfigKey<List<String>> LABELS = ConfigKeys.newConfigKey(
            new TypeToken<List<String>>() { },
            "docker.constraint.labels",
            "The list of required labels", ImmutableList.<String>of());

    @Override
    public boolean apply(DockerHostLocation input) {
        Set<String> labels = MutableSet.copyOf(config().get(LABELS));
        if (labels.isEmpty()) return true;
        SshMachineLocation ssh = input.getMachine();
        if (ssh instanceof JcloudsSshMachineLocation) {
            JcloudsSshMachineLocation jclouds = (JcloudsSshMachineLocation) ssh;
            NodeMetadata metadata = jclouds.getOptionalNode().get();
            Set<String> tags = MutableSet.copyOf(Iterables.transform(metadata.getTags(), new Function<String, String>() {
                @Override
                public String apply(String input) {
                    return Iterables.get(Splitter.on("=").split(input), 0);
                }
            }));
            tags.addAll(metadata.getUserMetadata().keySet());
            labels.removeAll(tags);
            LOG.debug("Host {} : Tags {} : Remaining {}",
                    new Object[] { input, Iterables.toString(tags), labels.isEmpty() ? "none" : Iterables.toString(labels) });
            return labels.isEmpty();
        } else {
            return false;
        }
    }
}
