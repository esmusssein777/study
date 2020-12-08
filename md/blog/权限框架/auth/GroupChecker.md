@Component
@Qualifier("groupChecker")
public class GroupChecker extends DataChecker<Group> {

    @Autowired
    private GroupPool groupPool;

    @Override
    public void check(Group data) {
        throw new UnsupportedOperationException("not support this method.");
    }

    @Override
    public void check(Collection<Group> dataCollection) {
        throw new UnsupportedOperationException("not support this method.");
    }

    public void checkUserId(Integer userId, Integer currentUserGroupId) {
        Group singleGroupWithDirectUsers = groupPool.getGroupTreeStructureWithDirectUsersBy(currentUserGroupId);
        Optional<Integer> hasCurrentUser = singleGroupWithDirectUsers.getAllUsers().stream()
                                                    .map(User::getId)
                                                    .filter(id -> id.equals(userId))
                                                    .findAny();

        if (!hasCurrentUser.isPresent()) {
            throw new ForbiddenException(ErrorCode.ACCESS_DENIED, "Can not access duty schedule of this user");
        }
    }

    public void checkId(Integer currentUserGroupId, Integer groupId) {
        if (Objects.equals(currentUserGroupId, groupId)) {
            return;
        }
        Group group = groupPool.getGroupTreeStructureWithoutDirectUsersBy(groupId);
        while ((group = group.getParentGroup()) != null) {
            if (Objects.equals(group.getId(), currentUserGroupId)) {
                return;
            }
        }
        throw new DataAuthenticationException("No privilege to access group(id=" + groupId + ")");
    }
}
