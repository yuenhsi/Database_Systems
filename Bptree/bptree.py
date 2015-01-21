'''
Implementing a simulation of a B+-tree in Python
Yuen Hsi Chang
'''

import math
from Node import Node

class Bptree:
	def insert(self, key, value):
		if self.getValue(key) == None:
			self.insertMain(self.root, key, value)
		else:
			raise Exception("This key already exists in the tree!")

	def insertMain(self, subtree, key, value):
		if subtree.type == 'leafType':
			if subtree.hasSpace():
				# add it to the leaf node directly
				subtree.add(key, value)
				return None
			else:
				# split the leaf node
				subtree.add(key,value)
				newNode1 = Node(self.nodeSize, 'leafType')
				newNode2 = Node(self.nodeSize, 'leafType')
				keyList = subtree.keyList
				pointerList = subtree.pointerList
				# entries to move to the left and right node
				nodesForLeft = math.floor(len(subtree.keyList) / 2.0)
				nodesForRight = len(subtree.keyList) - nodesForLeft
				counter = 0
				for i in range(len(subtree.keyList)):
					if i < nodesForLeft:
						newNode1.add(keyList[i], pointerList[i])
					else:
						newNode2.add(keyList[i], pointerList[i])
				if subtree is self.root:
					newRoot = Node(self.nodeSize, 'rootType')
					newRoot.add(newNode2.keyList[0], newNode1)
					newRoot.pointerList.append(newNode2)
					self.root = newRoot
				return (newNode2.keyList[0], newNode2, newNode1)
		else: 
			# i is the subtree to insert K into
			for sub in range(len(subtree.keyList)):
				if subtree.keyList[sub] > key:
					i = subtree.pointerList[sub]
					break
				else:
					i = subtree.pointerList[sub + 1]
			newEntry = self.insertMain(i, key, value)

			if newEntry == None:
				# child was not split, so nothing to do here
				return None
			else:
				# if there is space in this subtree
				if subtree.hasSpace():
					subtree.addSubtree(newEntry[0], newEntry[1], newEntry[2])
					return None
				else:
					# split this subtree
					subtree.addSubtree(newEntry[0], newEntry[1], newEntry[2])
					subtree1 = Node(self.nodeSize, 'rootType')
					subtree2 = Node(self.nodeSize, 'rootType')
					nodesForLeft = int(math.floor( (len(subtree.keyList) - 1) / 2.0))
					nodesForRight = int(len(subtree.keyList) - nodesForLeft- 1 )
					subtree1.keyList = [None] * nodesForLeft
					subtree1.pointerList = [None] * (nodesForLeft + 1)
					subtree2.keyList = [None] * nodesForRight
					subtree2.pointerList = [None] * (nodesForRight + 1)
					keyToMove = subtree.keyList[nodesForLeft]

					for a in range(int(nodesForLeft)):
						subtree1.keyList[a] = subtree.keyList[a]
						subtree1.pointerList[a] = subtree.pointerList[a]
					subtree1.pointerList[nodesForLeft] = subtree.pointerList[nodesForLeft]

					for a in range(nodesForRight):
						subtree2.keyList[a] = subtree.keyList[a + nodesForLeft + 1]
						subtree2.pointerList[a] = subtree.pointerList[a + nodesForLeft + 1]
					subtree2.pointerList[nodesForRight] = subtree.pointerList[len(subtree.keyList)]
					newEntry = (keyToMove, subtree2, subtree1)
					if subtree is self.root:
						newNode = Node(self.nodeSize, 'rootType')
						newNode.add(keyToMove, subtree1)
						newNode.pointerList.append(subtree2)
						self.root = newNode
					return newEntry

	def getValue(self, key): 
		currentNode = self.root
		while currentNode.type != 'leafType':
			if (key >= currentNode.keyList[len(currentNode.keyList) - 1]):
				currentNode = currentNode.pointerList[len(currentNode.keyList)]
			else:
				for i in range(len(currentNode.keyList)):
					if key < currentNode.keyList[i]:
						currentNode = currentNode.pointerList[i]
						break
		for i in range(len(currentNode.keyList)):
			if currentNode.keyList[i] == key:
				return currentNode.pointerList[i]
		return None

	def printTree(self):
		self.printTreeMain(self.root, '')

	def printTreeMain(self, root, space):
		print space + str(root.keyList)
		if (root.type == 'rootType'):
			space = space + '    '
			for i in root.pointerList:
				self.printTreeMain(i, space)
		else:
			print space + str(root.pointerList)

	def __init__(self, nodeSize):
		self.nodeSize = nodeSize
		self.root = Node(nodeSize, 'leafType')

def main():	
	return

main()