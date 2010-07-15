/*
 * File: DefaultMutableTreeNode.java
 * Version: 1.0
 * Initial Creation: May 12, 2010 8:06:48 AM
 *
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 * 
 *     http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * 
 * @author Anton Avtamonov
 */
package com.sun.pdfview.helper;

import java.util.Vector;

import net.rim.device.api.ui.component.TreeField;

/**
 * A <code>DefaultMutableTreeNode</code> is a general-purpose node in a tree data structure. In the context of PDF Renderer only a subset of functions are included.
 * @author Anton Avtamonov, Vincent Simonetti
 */
public class DefaultMutableTreeNode
{
	/** this node's parent, or null if this node has no parent*/
	protected DefaultMutableTreeNode parent;
	/** array of children, may be null if this node has no children*/
	protected Vector children;
	
	/**
	 * Creates a tree node that has no parent and no children, but which allows children.
	 */
	public DefaultMutableTreeNode()
	{
	}
	
	/**
	 * Removes <code>child</code> from its present parent (if it has a parent), sets the child's parent to this node, and then adds the child to this node's child array at index <code>childIndex</code>. <code>child</code> must not be null and must not be an ancestor of this node.
	 * @param child The DefaultMutableTreeNode to insert under this node.
	 * @param childIndex The index in this node's child array where this node is to be inserted.
	 * @throws ArrayIndexOutOfBoundsException If <code>childIndex</code> is out of bounds.
	 * @throws IllegalArgumentException If <code>child</code> is null or is an ancestor of this node.
	 */
	public void insert(final DefaultMutableTreeNode child, final int childIndex)
	{
		if(child == null || isNodeAncestor(child))
		{
			throw new IllegalArgumentException("child is null or is an ancestor to this node.");
		}
		
		if (child.getParent() instanceof DefaultMutableTreeNode)
		{
			child.getParent().remove(child);
		}
		child.setParent(this);
		getChildren().insertElementAt(child, childIndex);
	}
	
	/**
	 * Removes the child at the specified index from this node's children and sets that node's parent to null. The child node to remove must be a <code>DefaultMutableTreeNode</code>.
	 * @param childIndex The index in this node's child array of the child to remove.
	 * @throws ArrayIndexOutOfBoundsException If childIndex is out of bounds.
	 */
	public void remove(final int childIndex)
	{
		Vector childr = getChildren();
		DefaultMutableTreeNode child = (DefaultMutableTreeNode)childr.elementAt(childIndex);
		childr.removeElementAt(childIndex);
		child.setParent(null);
	}
	
	/**
	 * Sets this node's parent to <code>parent</code> but does not change the parent's child array. This method is called from <code>insert()</code> and 
	 * <code>remove()</code> to reassign a child's parent, it should not be messaged from anywhere else.
	 * @param parent This node's new parent.
	 */
	public void setParent(final DefaultMutableTreeNode parent)
	{
		this.parent = parent;
	}
	
	/**
	 * Returns this node's parent or null if this node has no parent.
	 * @return This node's parent DefaultMutableTreeNode, or null if this node has no parent.
	 */
	public DefaultMutableTreeNode getParent()
	{
		return this.parent;
	}
	
	/**
	 * Returns the number of children of this node.
	 * @return An int giving the number of children of this node.
	 */
	public int getChildCount()
	{
		return children != null ? children.size() : 0;
	}
	
	/**
	 * Removes <code>child</code> from this node's child array, giving it a null parent.
	 * @param child A child of this node to remove.
	 * @throws IllegalArgumentException If <code>child</code> is null or is not a child of this node.
	 */
	public void remove(final DefaultMutableTreeNode child)
	{
		int index = -1;
		if (child == null || children == null || (index = children.indexOf(child)) == -1)
		{
			throw new IllegalArgumentException("child is null or not a child of this node.");
		}
		remove(index);
	}
	
	/**
	 * Removes <code>child</code> from its parent and makes it a child of this node by adding it to the end of this node's child array.
	 * @param child Node to add as a child of this node.
	 * @throws IllegalArgumentException If <code>child</code> is null.
	 */
	public void add(final DefaultMutableTreeNode child)
	{
		insert(child, getChildCount() - (isNodeChild(child) ? 1 : 0));
	}
	
	/**
	 * Returns true if <code>anotherNode</code> is an ancestor of this node -- if it is this node, this node's parent, or an ancestor of this node's parent. (Note that 
	 * a node is considered an ancestor of itself.) If <code>anotherNode</code> is null, this method returns false. This operation is at worst O(h) where h is the 
	 * distance from the root to this node.
	 * @param anotherNode Node to test as an ancestor of this node.
	 * @return true if this node is a descendant of <code>anotherNode</code>.
	 */
	public boolean isNodeAncestor(final DefaultMutableTreeNode anotherNode)
	{
		if (anotherNode == null)
		{
			return false;
		}
		DefaultMutableTreeNode currentParent = this;
		while(currentParent != null)
		{
			if(currentParent == anotherNode)
			{
				return true;
			}
			currentParent = currentParent.getParent();
		}
		
		return false;
	}
	
	/**
	 * Returns true if <code>child</code> is a child of this node. If <code>child</code> is null, this method returns false.
	 * @return true if <code>child</code> is a child of this node; false if <code>child</code> is null.
	 */
	public boolean isNodeChild(final DefaultMutableTreeNode child)
	{
		return child != null && children != null ? children.contains(child) : false;
	}
	
	/**
	 * Returns the child at the specified index in this node's child array.
	 * @param index An index into this node's child array.
	 * @return The DefaultMutableTreeNode in this node's child array at the specified index.
	 * @throws ArrayIndexOutOfBoundsException If index is out of bounds.
	 */
	public DefaultMutableTreeNode getChildAt(int index)
	{
		return (DefaultMutableTreeNode)getChildren().elementAt(index);
	}
	
	private Vector getChildren()
	{
		if(this.children == null)
		{
			this.children = new Vector();
		}
		return this.children;
	}
	
	/**
	 * Load this tree node into a TreeField as the root element.
	 * @param tree The TreeField to load this tree node in to.
	 */
	public void loadTree(TreeField tree)
	{
		//Always load as root
		loadChild(0, tree, this);
	}
	
	private void loadChild(int parentID, TreeField tree, DefaultMutableTreeNode child)
	{
		parentID = tree.addChildNode(parentID, child, false);
		if(child.children != null)
		{
			int count = child.children.size();
			for(int i = count - 1; i >= 0; i--)
			{
				loadChild(parentID, tree, (DefaultMutableTreeNode)child.children.elementAt(i));
			}
		}
	}
}
