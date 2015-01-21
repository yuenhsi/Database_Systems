# Yuen Hsi Chang

'''
Takes in a relation schema and a list of functional dependencies, and performs 
a lossless decomposition into BCNF. 
'''
def bcnf(attributes, fds):
    dependancies = closure(attributes, fds)
    result = [attributes]
    done = False
    while not done:
        for relation in result: 
            superKeys = getSuperKey(relation, dependancies)
            currentLoop = list(dependancies)
            for dependancy in currentLoop: 
                if dependancy[0] in superKeys:
                    currentLoop.remove(dependancy)
            currentLoop = removeIntersectingDependancies(currentLoop)
            print currentLoop
            print 'cur'
            if not len(currentLoop) == 0:
                alphaList = currentLoop[0][0]
                betaList = currentLoop[0][1]
                result.remove(relation)
                for beta in betaList:
                    if beta in relation:
                        relation.remove(beta)
                result.append(relation)
                alphaBeta = []
                for alpha in alphaList:
                    alphaBeta.append(alpha)
                for beta in betaList:
                    alphaBeta.append(beta)
                result.append(alphaBeta)
                print result
                print 'resultabove'
                relationOK = False
            else:
                mylen = len(result) - 1
                #if relation == result[mylen]:
                done = True
                relationOK = True
            if not relationOK:
                break
            done = True
    return result

def getSuperKey(relation, dependancies):
    superKey = []
    for dependancy in dependancies:
        if set(dependancy[1]) == set(relation):
            superKey.append(dependancy[0])
    return superKey

def removeIntersectingDependancies(dependancyList):
    cList = list(dependancyList)
    for dependancy in dependancyList:
        if not set.intersection(set(dependancy[1]),set(dependancy[0])) == set():
            cList.remove(dependancy)
    return cList

# finds the closure given a set of attributes and function dependancies
def closure(attributes, fds):
    # find all reflexive dependancies formed by the relation and given FDs, adding them to F
    F = reflexivity(attributes, fds)
    # augment all dependancies, forming potential new dependancies for F
    for fDep in F:
        F = augmentation(attributes, fDep, F)
    # add all transitive dependancies that exist in F to F
    F = transitivity(F)
    # F now satisfies reflexivity, augmentation, and transitivity - the closure F+ has been computed
    return F

# finds all subsets of a given list iteratively
def findSubsets(alpha):
    result = [[]]
    for element in alpha:
        result.extend([subset + [element] for subset in result])
    result.remove([])
    return result

# reflexivity creates an initial F which consists of all reflexive dependancies formed by the 
# relation and given functional dependancies
def reflexivity(attributes, fds):
    F = list()
    alpha = attributes
    alphaSubsets = findSubsets(alpha)

    # for all possible subsets of the relation(alpha), find all of alpha's subsets(beta), and 
    # add (alpha, beta) to the dependancy. 
    index = 0
    while index < len(alphaSubsets):
        alpha = alphaSubsets[index]
        betaSubsets = findSubsets(alpha)
        for beta in betaSubsets:
            newDependancy = (alpha, beta)
            F.append((newDependancy),)
        index = index + 1

    # for all alphas of the given functional dependencies, find all of it's subsets (beta), and add 
    # (alpha, beta) to the dependancy if it does not already exist. 
    for element in fds:
        fdSubsets = findSubsets(element[1])
        for beta in fdSubsets:
            newDep = (element[0], beta)
            if newDep not in F:
                F.append((newDep),)
    return F

# given a dependancy, augmentation finds all of it's augmented dependancies and adds them to F
def augmentation(attributes, fDep, F):
    result = list(F)
    allSubsets = findSubsets(attributes)
    index = 0
    # creates a set of subsets gamma, iterates through all gamma, adding it to both alpha and beta. 
    while index < len(allSubsets):
        subset = allSubsets[index]
        for gamma in subset:
            alpha = list(fDep[0])
            beta = list(fDep[1])
            if gamma not in alpha:
                alpha.append(gamma)
                alpha.sort()
            if gamma not in beta:
                beta.append(gamma)
                beta.sort()
            new = (alpha, beta)
            # if the new dependancy (alpha + gamma, beta + gamma) is not in F, add it to F
            if new not in result:
                result.append((new),)
        index = index + 1
    return result

# given any pair of dependancies in F, if transitivity is satisfied, add the new dependancy to F
def transitivity(F):
    result = list(F)
    # iterate through F, searching for all possible pairs of dependancies
    for alpha in result:
        for beta in result:
            # if transitivity is satisfied for a given pair e.g. (alpha, beta) and (beta, lambda), 
            # add (alpha, lambda) to F given that (alpha, lambda) does not already exist. 
            if set(alpha[1]) == set(beta[0]):
                newDependancy = (alpha[0], beta[1])
                if newDependancy not in result:
                    result.append((newDependancy),)
    return result

def main():
    fd1 = ([3],[4])
    fd2 = ([1],[2])
    allfds = (fd2,fd1)
    print(closure([1,2,3,4], allfds))
main()