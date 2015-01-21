# Yuen Hsi Chang

class Node:
	def add(self, key, pointer):
		counter = 0
		for i in self.keyList:
			if key < i:
				self.keyList.insert(counter, key)
				self.pointerList.insert(counter, pointer)
				return
			counter = counter + 1
		else:
			self.keyList.insert(counter, key)
			self.pointerList.insert(counter, pointer)

	def addSubtree(self, key, subTree, subTreeLeft):
		counter = 0
		for i in self.keyList:
			if key < i:
				self.keyList.insert(counter, key)
				self.pointerList[counter] = subTreeLeft
				self.pointerList.insert(counter + 1, subTree)
				return
			counter = counter + 1
		else:
			self.keyList.insert(counter, key)
			self.pointerList[counter] = subTreeLeft
			self.pointerList.insert(counter + 1, subTree)

	# only to be used if this is of leafType
	def hasSpace(self):
		if len(self.keyList) < self.nodeSize:
			return True
		else:
			return False

	def __init__(self, nodeSize, nodeType):
		self.nodeSize = nodeSize
		self.keyList = []
		self.pointerList = []
		self.type = nodeType