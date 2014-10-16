/*
 * Copyright (c) 2007-2014 Concurrent, Inc. All Rights Reserved.
 *
 * Project and contact information: http://www.cascading.org/
 *
 * This file is part of the Cascading project.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cascading.flow.planner.process;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import cascading.flow.FlowStep;
import cascading.flow.planner.BaseFlowStep;
import cascading.flow.planner.FlowPlanner;
import cascading.flow.planner.graph.ElementGraph;
import cascading.flow.planner.graph.FlowElementGraph;

public class FlowStepGraph extends ProcessGraph<FlowStep>
  {
  private transient String tracePath;

  public FlowStepGraph()
    {
    }

  public FlowStepGraph( String tracePath, FlowPlanner<?, ?> flowPlanner, FlowElementGraph flowElementGraph, Map<ElementGraph, List<? extends ElementGraph>> nodeSubGraphsMap, Map<ElementGraph, List<? extends ElementGraph>> pipelineSubGraphsMap )
    {
    this.tracePath = tracePath;

    buildGraph( flowPlanner, flowElementGraph, nodeSubGraphsMap, pipelineSubGraphsMap );

    Iterator<FlowStep> iterator = getTopologicalIterator();

    int ordinal = 0;
    int size = vertexSet().size();

    while( iterator.hasNext() )
      {
      BaseFlowStep flowStep = (BaseFlowStep) iterator.next();

      flowStep.setOrdinal( ordinal );
      flowStep.setName( flowPlanner.makeFlowStepName( flowStep, size, ordinal ) );

      ElementGraph stepSubGraph = flowStep.getElementGraph();

      writePlan( ordinal, stepSubGraph, nodeSubGraphsMap.get( stepSubGraph ), pipelineSubGraphsMap );

      ordinal++;
      }
    }

  protected void buildGraph( FlowPlanner<?, ?> flowPlanner, FlowElementGraph flowElementGraph, Map<ElementGraph, List<? extends ElementGraph>> nodeSubGraphsMap, Map<ElementGraph, List<? extends ElementGraph>> pipelineSubGraphsMap )
    {
    for( ElementGraph stepSubGraph : nodeSubGraphsMap.keySet() )
      {
      List<? extends ElementGraph> nodeSubGraphs = nodeSubGraphsMap.get( stepSubGraph );
      FlowNodeGraph flowNodeGraph = createFlowNodeGraph( flowPlanner, flowElementGraph, pipelineSubGraphsMap, nodeSubGraphs );
      FlowStep flowStep = flowPlanner.createFlowStep( stepSubGraph, flowNodeGraph );

      addVertex( flowStep );
      }

    bindEdges();
    }

  protected FlowNodeGraph createFlowNodeGraph( FlowPlanner<?, ?> flowPlanner, FlowElementGraph flowElementGraph, Map<ElementGraph, List<? extends ElementGraph>> pipelineSubGraphsMap, List<? extends ElementGraph> nodeSubGraphs )
    {
    return new FlowNodeGraph( flowPlanner, flowElementGraph, nodeSubGraphs, pipelineSubGraphsMap );
    }

  private void writePlan( int stepCount, ElementGraph stepSubGraph, List<? extends ElementGraph> nodeSubGraphs, Map<ElementGraph, List<? extends ElementGraph>> pipelineSubGraphsMap )
    {
    if( getTracePath() == null )
      return;

    String rootPath = getTracePath() + "/steps";
    String stepGraphName = String.format( "%s/%04d-step-sub-graph.dot", rootPath, stepCount );

    stepSubGraph.writeDOT( stepGraphName );

    for( int i = 0; i < nodeSubGraphs.size(); i++ )
      {
      ElementGraph nodeGraph = nodeSubGraphs.get( i );
      String nodeGraphName = String.format( "%s/%04d-%04d-step-node-sub-graph.dot", rootPath, stepCount, i );

      nodeGraph.writeDOT( nodeGraphName );

      List<? extends ElementGraph> pipelineGraphs = pipelineSubGraphsMap.get( nodeGraph );

      if( pipelineGraphs == null )
        continue;

      for( int j = 0; j < pipelineGraphs.size(); j++ )
        {
        ElementGraph pipelineGraph = pipelineGraphs.get( j );

        String pipelineGraphName = String.format( "%s/%04d-%04d-%04d-step-node-pipeline-sub-graph.dot", rootPath, stepCount, i, j );

        pipelineGraph.writeDOT( pipelineGraphName );
        }

      }
    }

  private String getTracePath()
    {
    return tracePath;
    }
  }