import bptree

b = bptree.Bptree(2)  
b.insert(2,"hello")
b.insert(7,"hello")
b.insert(22,"hello")
b.insert(24,"ascd")
b.insert(16,"hello")
b.insert(3,"wqesa")
b.insert(5,"hello")
b.insert(14,"eq")
b.insert(29,"eqwceqw")
b.insert(19,"hello")
b.insert(31,"hello")
b.insert(23,"asdfc")
b.insert(28,"hello")
b.insert(39,"asda")
b.insert(54,"hello")
b.printTree()
print b.getValue(39)
print b.getValue(19)
print b.getValue(29)
print b.getValue(219)